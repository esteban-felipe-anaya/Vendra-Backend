package com.vendra.order;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class PricingServiceTest {

  @Test
  void lineTotalMultipliesAndRounds() {
    assertThat(PricingService.lineTotal(new BigDecimal("12.50"), 3))
        .isEqualByComparingTo("37.50");
  }

  @Test
  void commissionAndPayoutAreConsistent() {
    BigDecimal subtotal = new BigDecimal("189.00");
    BigDecimal rate = new BigDecimal("0.1000");
    BigDecimal commission = PricingService.commission(subtotal, rate);
    assertThat(commission).isEqualByComparingTo("18.90");
    assertThat(PricingService.shopPayout(subtotal, commission)).isEqualByComparingTo("170.10");
  }

  @Test
  void moneyRoundsHalfUpTo2dp() {
    assertThat(PricingService.money(new BigDecimal("1.005"))).isEqualByComparingTo("1.01");
  }
}
