package com.vendra.order;

import com.vendra.domain.*;
import com.vendra.dto.Dtos.*;
import com.vendra.repo.*;
import java.util.List;
import org.springframework.stereotype.Component;

/** Assembles order/sub-order/delivery DTOs from the persisted rows. */
@Component
public class OrderMapper {

  private final SubOrderRepository subOrders;
  private final OrderItemRepository orderItems;
  private final DeliveryRepository deliveries;
  private final CourierLocationRepository locations;
  private final ShopRepository shops;
  private final CourierRepository couriers;
  private final ProfileRepository profiles;

  public OrderMapper(
      SubOrderRepository subOrders, OrderItemRepository orderItems, DeliveryRepository deliveries,
      CourierLocationRepository locations, ShopRepository shops, CourierRepository couriers,
      ProfileRepository profiles) {
    this.subOrders = subOrders;
    this.orderItems = orderItems;
    this.deliveries = deliveries;
    this.locations = locations;
    this.shops = shops;
    this.couriers = couriers;
    this.profiles = profiles;
  }

  public OrderDto toOrderDto(Order o) {
    List<SubOrderDto> subs =
        subOrders.findByOrderId(o.getId()).stream().map(this::toSubOrderDto).toList();
    return new OrderDto(o.getId(), o.getCustomerId().toString(), o.getStatus(),
        o.getPaymentStatus(), o.getItemsSubtotal(), o.getDeliveryFee(), o.getTax(),
        o.getDiscount(), o.getTotal(), o.getShipTo(), subs, o.getCreatedAt());
  }

  public SubOrderDto toSubOrderDto(SubOrder so) {
    List<OrderItemDto> items =
        orderItems.findBySubOrderId(so.getId()).stream()
            .map(i -> new OrderItemDto(i.getId(), i.getProductId(), i.getNameSnapshot(),
                i.getImageSnapshot(), i.getUnitPrice(), i.getQuantity(), i.getLineTotal()))
            .toList();
    DeliveryDto delivery = deliveries.findBySubOrderId(so.getId()).map(this::toDeliveryDto).orElse(null);
    String shopName = shops.findById(so.getShopId()).map(Shop::getName).orElse("Shop");
    return new SubOrderDto(so.getId(), so.getOrderId(), so.getShopId(), shopName, so.getStatus(),
        so.getItemsSubtotal(), so.getShopPayout(), so.getPayoutStatus(), items, delivery,
        so.getCreatedAt());
  }

  public DeliveryDto toDeliveryDto(Delivery d) {
    String courierName =
        d.getCourierId() == null ? null
            : couriers.findById(d.getCourierId())
                .flatMap(c -> profiles.findById(c.getProfileId()))
                .map(Profile::getFullName).orElse(null);
    LocationDto last =
        d.getCourierId() == null ? null
            : locations.findByDeliveryIdOrderByRecordedAtDesc(d.getId()).stream().findFirst()
                .map(l -> new LocationDto(l.getLat(), l.getLng(), l.getHeading(), l.getRecordedAt()))
                .orElse(null);
    return new DeliveryDto(d.getId(), d.getSubOrderId(), d.getOrderId(), d.getCourierId(),
        courierName, d.getStatus(), d.getPickupLat(), d.getPickupLng(), d.getPickupLabel(),
        d.getDropoffLat(), d.getDropoffLng(), d.getDropoffLabel(), d.getCourierFee(), last);
  }
}
