package com.vendra.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "platform_settings")
@Getter
@Setter
@NoArgsConstructor
public class PlatformSettings {
  @Id private String id = "platform";

  @Column(nullable = false)
  private BigDecimal commissionRate = new BigDecimal("0.1000");

  @Column(nullable = false)
  private BigDecimal baseDeliveryFee = new BigDecimal("4.99");

  @Column(nullable = false)
  private BigDecimal courierFeeShare = new BigDecimal("0.8000");

  @Column(nullable = false)
  private BigDecimal taxRate = new BigDecimal("0.0000");

  @Column(nullable = false)
  private String currency = "usd";

  @UpdateTimestamp
  private OffsetDateTime updatedAt;
}
