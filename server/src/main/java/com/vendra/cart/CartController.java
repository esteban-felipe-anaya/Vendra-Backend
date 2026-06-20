package com.vendra.cart;

import com.vendra.dto.Dtos.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** Customer cart. */
@RestController
@RequestMapping("/api/v1/cart")
@PreAuthorize("hasRole('customer')")
@Tag(name = "Cart", description = "Multi-shop customer cart")
public class CartController {

  private final CartService cart;

  public CartController(CartService cart) {
    this.cart = cart;
  }

  @GetMapping
  @Operation(summary = "Get my cart grouped by shop")
  public CartDto get() {
    return cart.view();
  }

  @PostMapping("/items")
  @Operation(summary = "Add an item to the cart")
  public CartDto add(@Valid @RequestBody AddToCartRequest req) {
    return cart.add(req);
  }

  @PatchMapping("/items/{itemId}")
  @Operation(summary = "Update a cart item quantity")
  public CartDto update(@PathVariable String itemId, @Valid @RequestBody UpdateCartItemRequest req) {
    return cart.updateItem(itemId, req.quantity());
  }

  @DeleteMapping("/items/{itemId}")
  @Operation(summary = "Remove a cart item")
  public CartDto remove(@PathVariable String itemId) {
    return cart.removeItem(itemId);
  }

  @DeleteMapping
  @Operation(summary = "Clear the cart")
  public MessageResponse clear() {
    cart.clear();
    return new MessageResponse("Cart cleared");
  }
}
