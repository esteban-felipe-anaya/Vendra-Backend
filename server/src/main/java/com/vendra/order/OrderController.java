package com.vendra.order;

import com.vendra.dto.Dtos.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** Customer order history + tracking. */
@RestController
@RequestMapping("/api/v1/orders")
@PreAuthorize("hasRole('customer')")
@Tag(name = "Orders (customer)", description = "Order history and tracking")
public class OrderController {

  private final OrderService orders;

  public OrderController(OrderService orders) {
    this.orders = orders;
  }

  @GetMapping
  @Operation(summary = "List my orders")
  public List<OrderDto> list() {
    return orders.myOrders();
  }

  @GetMapping("/{id}")
  @Operation(summary = "Get one of my orders with live sub-order + delivery state")
  public OrderDto get(@PathVariable String id) {
    return orders.myOrder(id);
  }

  @PostMapping("/{id}/cancel")
  @Operation(summary = "Cancel an order (only while placed/accepted)")
  public OrderDto cancel(@PathVariable String id) {
    return orders.cancel(id);
  }
}
