package com.vendra.dispatch;

import com.vendra.account.AccountService;
import com.vendra.common.ApiException;
import com.vendra.domain.*;
import com.vendra.dto.Dtos.*;
import com.vendra.notify.NotificationService;
import com.vendra.order.OrderMapper;
import com.vendra.order.OrderStateMachine;
import com.vendra.order.PricingService;
import com.vendra.payment.PayoutService;
import com.vendra.repo.*;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Courier dispatch: turns a ready sub-order into a delivery offer broadcast to available couriers,
 * runs the first-accept-wins claim, drives the courier side of the state machine, and records
 * location pings for the live map. On delivered it settles shop + courier payouts.
 */
@Service
public class DispatchService {

  private final DeliveryRepository deliveries;
  private final SubOrderRepository subOrders;
  private final OrderRepository orders;
  private final CourierLocationRepository locations;
  private final CourierRepository couriers;
  private final ShopRepository shops;
  private final AddressRepository addresses;
  private final PlatformSettingsRepository settingsRepo;
  private final AccountService account;
  private final NotificationService notifications;
  private final PayoutService payouts;
  private final OrderMapper mapper;

  public DispatchService(
      DeliveryRepository deliveries, SubOrderRepository subOrders, OrderRepository orders,
      CourierLocationRepository locations, CourierRepository couriers, ShopRepository shops,
      AddressRepository addresses, PlatformSettingsRepository settingsRepo, AccountService account,
      NotificationService notifications, PayoutService payouts, OrderMapper mapper) {
    this.deliveries = deliveries;
    this.subOrders = subOrders;
    this.orders = orders;
    this.locations = locations;
    this.couriers = couriers;
    this.shops = shops;
    this.addresses = addresses;
    this.settingsRepo = settingsRepo;
    this.account = account;
    this.notifications = notifications;
    this.payouts = payouts;
    this.mapper = mapper;
  }

  /** Called when a sub-order becomes ready — create + broadcast a delivery offer. */
  @Transactional
  public void createOffer(SubOrder so) {
    if (deliveries.findBySubOrderId(so.getId()).isPresent()) return;
    PlatformSettings s = settingsRepo.findById("platform").orElseThrow();
    Order order = orders.findById(so.getOrderId()).orElseThrow();
    Shop shop = shops.findById(so.getShopId()).orElse(null);
    Address addr = order.getAddressId() == null ? null
        : addresses.findById(order.getAddressId()).orElse(null);

    Delivery d = new Delivery();
    d.setSubOrderId(so.getId());
    d.setOrderId(order.getId());
    d.setStatus("offered");
    d.setPickupLabel(shop != null ? shop.getName() : "Shop");
    if (addr != null) {
      d.setDropoffLat(addr.getLat());
      d.setDropoffLng(addr.getLng());
      d.setDropoffLabel(addr.getLine1());
    }
    d.setCourierFee(PricingService.courierFee(s));
    deliveries.save(d);

    // mirror sub-order state to courier_assigned-pending: it stays 'ready' until claimed.
    // notify available couriers (Realtime on deliveries also surfaces the offered row)
    for (Courier c : couriers.findByAvailability("available")) {
      notifications.notify(c.getProfileId(), "dispatch_offer", "New delivery offer",
          "Pickup at " + d.getPickupLabel() + " — $" + d.getCourierFee() + " fee.",
          "delivery", d.getId());
    }
  }

  // ---- courier-facing ----
  public List<DeliveryDto> offers() {
    Courier me = account.currentCourier();
    if (!"available".equals(me.getAvailability())) return List.of();
    return deliveries.findByStatus("offered").stream().map(mapper::toDeliveryDto).toList();
  }

  public List<DeliveryDto> myDeliveries() {
    Courier me = account.currentCourier();
    return deliveries.findByCourierId(me.getId()).stream().map(mapper::toDeliveryDto).toList();
  }

  @Transactional
  public DeliveryDto accept(String deliveryId) {
    Courier me = account.currentCourier();
    int won = deliveries.claim(deliveryId, me.getId(), OffsetDateTime.now());
    if (won == 0) throw ApiException.conflict("This delivery was already taken");

    Delivery d = deliveries.findById(deliveryId).orElseThrow();
    SubOrder so = subOrders.findById(d.getSubOrderId()).orElseThrow();
    OrderStateMachine.assertTransition(so.getStatus(), OrderStateMachine.COURIER_ASSIGNED, "courier");
    so.setStatus(OrderStateMachine.COURIER_ASSIGNED);
    subOrders.save(so);

    me.setAvailability("busy");
    couriers.save(me);

    Order order = orders.findById(d.getOrderId()).orElseThrow();
    notifications.notify(order.getCustomerId(), "delivery_update", "Courier assigned",
        "A courier is heading to pick up your order.", "order", order.getId());
    return mapper.toDeliveryDto(d);
  }

  /** Courier advances picked_up → on_the_way → delivered. */
  @Transactional
  public DeliveryDto transition(String deliveryId, String to) {
    Courier me = account.currentCourier();
    Delivery d = deliveries.findById(deliveryId).orElseThrow(() -> ApiException.notFound("Delivery"));
    if (!me.getId().equals(d.getCourierId())) throw ApiException.forbidden("Not your delivery");

    SubOrder so = subOrders.findById(d.getSubOrderId()).orElseThrow();
    OrderStateMachine.assertTransition(so.getStatus(), to, "courier");
    OffsetDateTime now = OffsetDateTime.now();
    so.setStatus(to);
    d.setStatus(deliveryStatusFor(to));
    switch (to) {
      case OrderStateMachine.PICKED_UP -> d.setPickedUpAt(now);
      case OrderStateMachine.DELIVERED -> {
        d.setDeliveredAt(now);
        so.setDeliveredAt(now);
      }
      default -> {}
    }
    subOrders.save(so);
    deliveries.save(d);

    Order order = orders.findById(d.getOrderId()).orElseThrow();
    notifications.notify(order.getCustomerId(), "delivery_update", "Order " + to.replace('_', ' '),
        "Your order is now " + to.replace('_', ' ') + ".", "order", order.getId());

    if (OrderStateMachine.DELIVERED.equals(to)) {
      payouts.settleShop(so);
      payouts.settleCourier(d);
      me.setAvailability("available");
      couriers.save(me);
      maybeCompleteOrder(order);
    }
    return mapper.toDeliveryDto(d);
  }

  @Transactional
  public void ping(String deliveryId, LocationPingRequest req) {
    Courier me = account.currentCourier();
    Delivery d = deliveries.findById(deliveryId).orElseThrow(() -> ApiException.notFound("Delivery"));
    if (!me.getId().equals(d.getCourierId())) throw ApiException.forbidden("Not your delivery");
    CourierLocation loc = new CourierLocation();
    loc.setDeliveryId(deliveryId);
    loc.setCourierId(me.getId());
    loc.setLat(req.lat());
    loc.setLng(req.lng());
    loc.setHeading(req.heading());
    locations.save(loc);
    me.setCurrentLat(req.lat());
    me.setCurrentLng(req.lng());
    couriers.save(me);
  }

  @Transactional
  public void setAvailability(String availability) {
    if (!List.of("offline", "available", "busy").contains(availability))
      throw ApiException.badRequest("Invalid availability");
    Courier me = account.currentCourier();
    me.setAvailability(availability);
    couriers.save(me);
  }

  private void maybeCompleteOrder(Order order) {
    boolean allDone =
        subOrders.findByOrderId(order.getId()).stream()
            .allMatch(s -> OrderStateMachine.isTerminal(s.getStatus()));
    if (allDone) {
      order.setStatus("completed");
      orders.save(order);
    }
  }

  private String deliveryStatusFor(String subOrderStatus) {
    return switch (subOrderStatus) {
      case OrderStateMachine.PICKED_UP -> "picked_up";
      case OrderStateMachine.ON_THE_WAY -> "on_the_way";
      case OrderStateMachine.DELIVERED -> "delivered";
      default -> "assigned";
    };
  }
}
