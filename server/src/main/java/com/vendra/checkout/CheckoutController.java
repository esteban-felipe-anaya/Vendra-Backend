package com.vendra.checkout;

import com.vendra.dto.Dtos.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** Checkout — quote and place an order (splits into per-shop sub-orders). */
@RestController
@RequestMapping("/api/v1/checkout")
@PreAuthorize("hasRole('customer')")
@Tag(name = "Checkout", description = "Quote and place multi-shop orders")
public class CheckoutController {

  private final CheckoutService checkout;

  public CheckoutController(CheckoutService checkout) {
    this.checkout = checkout;
  }

  @GetMapping("/quote")
  @Operation(summary = "Compute totals for the current cart (no order created)")
  public CheckoutQuoteDto quote(@RequestParam(required = false) String promoCode) {
    return checkout.quote(promoCode);
  }

  @PostMapping
  @Operation(summary = "Place the order; returns Stripe client secret for payment")
  public CheckoutResponse place(@Valid @RequestBody CheckoutRequest req) {
    return checkout.checkout(req);
  }
}
