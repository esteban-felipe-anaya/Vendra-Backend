package com.vendra.order;

import com.vendra.dto.Dtos.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** Merchant incoming-orders board + fulfillment transitions. */
@RestController
@RequestMapping("/api/v1/shop/orders")
@PreAuthorize("hasRole('shop')")
@Tag(name = "Orders (shop)", description = "Merchant order fulfillment")
public class ShopOrderController {

  private final OrderService orders;

  public ShopOrderController(OrderService orders) {
    this.orders = orders;
  }

  @GetMapping
  @Operation(summary = "List sub-orders for my shop")
  public List<SubOrderDto> list() {
    return orders.shopOrders();
  }

  @PostMapping("/{subOrderId}/transition")
  @Operation(summary = "Advance a sub-order: accepted | preparing | ready | rejected")
  public SubOrderDto transition(
      @PathVariable String subOrderId, @Valid @RequestBody TransitionRequest req) {
    return orders.shopTransition(subOrderId, req.to());
  }
}
