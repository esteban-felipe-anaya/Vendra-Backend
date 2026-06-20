package com.vendra.order;

import com.vendra.account.AccountService;
import com.vendra.common.ApiException;
import com.vendra.domain.Order;
import com.vendra.domain.Shop;
import com.vendra.domain.SubOrder;
import com.vendra.dispatch.DispatchService;
import com.vendra.dto.Dtos.*;
import com.vendra.notify.NotificationService;
import com.vendra.repo.OrderRepository;
import com.vendra.repo.SubOrderRepository;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Order views for customers and shops, plus the shop-driven part of the state machine. */
@Service
public class OrderService {

  private final OrderRepository orders;
  private final SubOrderRepository subOrders;
  private final OrderMapper mapper;
  private final AccountService account;
  private final NotificationService notifications;
  private final DispatchService dispatch;

  public OrderService(
      OrderRepository orders, SubOrderRepository subOrders, OrderMapper mapper,
      AccountService account, NotificationService notifications, @Lazy DispatchService dispatch) {
    this.orders = orders;
    this.subOrders = subOrders;
    this.mapper = mapper;
    this.account = account;
    this.notifications = notifications;
    this.dispatch = dispatch;
  }

  // ---- customer ----
  public List<OrderDto> myOrders() {
    return orders.findByCustomerIdOrderByCreatedAtDesc(account.currentUserId()).stream()
        .map(mapper::toOrderDto).toList();
  }

  public OrderDto myOrder(String id) {
    Order o = orders.findById(id).orElseThrow(() -> ApiException.notFound("Order"));
    if (!o.getCustomerId().equals(account.currentUserId())) throw ApiException.forbidden("Not your order");
    return mapper.toOrderDto(o);
  }

  @Transactional
  public OrderDto cancel(String id) {
    Order o = orders.findById(id).orElseThrow(() -> ApiException.notFound("Order"));
    if (!o.getCustomerId().equals(account.currentUserId())) throw ApiException.forbidden("Not your order");
    for (SubOrder so : subOrders.findByOrderId(id)) {
      if (List.of("placed", "accepted").contains(so.getStatus())) {
        OrderStateMachine.assertTransition(so.getStatus(), OrderStateMachine.CANCELLED, "customer");
        so.setStatus(OrderStateMachine.CANCELLED);
        subOrders.save(so);
      }
    }
    o.setStatus("cancelled");
    orders.save(o);
    return mapper.toOrderDto(o);
  }

  // ---- shop ----
  public List<SubOrderDto> shopOrders() {
    Shop shop = account.currentShop();
    return subOrders.findByShopIdOrderByCreatedAtDesc(shop.getId()).stream()
        .map(mapper::toSubOrderDto).toList();
  }

  @Transactional
  public SubOrderDto shopTransition(String subOrderId, String to) {
    Shop shop = account.currentShop();
    SubOrder so = subOrders.findById(subOrderId).orElseThrow(() -> ApiException.notFound("Sub-order"));
    if (!so.getShopId().equals(shop.getId())) throw ApiException.forbidden("Not your order");

    OrderStateMachine.assertTransition(so.getStatus(), to, "shop");
    so.setStatus(to);
    OffsetDateTime now = OffsetDateTime.now();
    switch (to) {
      case OrderStateMachine.ACCEPTED -> so.setAcceptedAt(now);
      case OrderStateMachine.READY -> so.setReadyAt(now);
      default -> {}
    }
    subOrders.save(so);

    // notify the customer
    Order parent = orders.findById(so.getOrderId()).orElseThrow();
    notifications.notify(parent.getCustomerId(), "order_update",
        "Order " + labelFor(to), "Your order from " + shopName(shop) + " is now " + to + ".",
        "sub_order", so.getId());

    // when ready, broadcast a delivery offer to available couriers
    if (OrderStateMachine.READY.equals(to)) {
      dispatch.createOffer(so);
    }
    return mapper.toSubOrderDto(so);
  }

  private String shopName(Shop s) {
    return s.getName();
  }

  private String labelFor(String status) {
    return switch (status) {
      case "accepted" -> "accepted";
      case "preparing" -> "being prepared";
      case "ready" -> "ready for pickup";
      case "rejected" -> "rejected";
      default -> "updated";
    };
  }
}
