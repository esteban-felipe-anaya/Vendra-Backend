package com.vendra.order;

import com.vendra.domain.PlatformSettings;
import com.vendra.domain.Promo;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.Optional;

/**
 * Pure server-side money math. Never trusts client totals. All amounts are in major currency units
 * (e.g. 12.50) and rounded to 2dp half-up.
 */
public final class PricingService {

  private PricingService() {}

  public static BigDecimal money(BigDecimal v) {
    return v.setScale(2, RoundingMode.HALF_UP);
  }

  public static BigDecimal lineTotal(BigDecimal unitPrice, int qty) {
    return money(unitPrice.multiply(BigDecimal.valueOf(qty)));
  }

  /** Commission a shop pays the platform on its sub-order subtotal. */
  public static BigDecimal commission(BigDecimal subtotal, BigDecimal rate) {
    return money(subtotal.multiply(rate));
  }

  /** Shop's payout = subtotal − commission. */
  public static BigDecimal shopPayout(BigDecimal subtotal, BigDecimal commission) {
    return money(subtotal.subtract(commission));
  }

  /** Courier fee = base delivery fee × courier share. Platform keeps the remainder. */
  public static BigDecimal courierFee(PlatformSettings s) {
    return money(s.getBaseDeliveryFee().multiply(s.getCourierFeeShare()));
  }

  public static BigDecimal tax(BigDecimal taxable, PlatformSettings s) {
    return money(taxable.multiply(s.getTaxRate()));
  }

  /** Apply a promo to an items subtotal, returning the discount amount (≥ 0, ≤ subtotal). */
  public static BigDecimal discount(BigDecimal itemsSubtotal, Optional<Promo> promo) {
    if (promo.isEmpty()) return BigDecimal.ZERO;
    Promo p = promo.get();
    OffsetDateTime now = OffsetDateTime.now();
    boolean active =
        p.isActive()
            && (p.getStartsAt() == null || !p.getStartsAt().isAfter(now))
            && (p.getEndsAt() == null || !p.getEndsAt().isBefore(now))
            && itemsSubtotal.compareTo(p.getMinSubtotal()) >= 0;
    if (!active) return BigDecimal.ZERO;
    BigDecimal d =
        "percent".equals(p.getDiscountType())
            ? itemsSubtotal.multiply(p.getAmount()).divide(BigDecimal.valueOf(100))
            : p.getAmount();
    return money(d.min(itemsSubtotal).max(BigDecimal.ZERO));
  }
}
