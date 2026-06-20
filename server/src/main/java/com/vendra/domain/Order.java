package com.vendra.domain;

import com.vendra.common.IdGenerator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
public class Order {
  @Id private String id;

  @Column(nullable = false)
  private UUID customerId;

  private String addressId;

  @JdbcTypeCode(SqlTypes.JSON)
  private String shipTo;

  @Column(nullable = false)
  private String currency = "usd";

  @Column(nullable = false)
  private BigDecimal itemsSubtotal = BigDecimal.ZERO;

  @Column(nullable = false)
  private BigDecimal deliveryFee = BigDecimal.ZERO;

  @Column(nullable = false)
  private BigDecimal tax = BigDecimal.ZERO;

  @Column(nullable = false)
  private BigDecimal discount = BigDecimal.ZERO;

  @Column(nullable = false)
  private BigDecimal total = BigDecimal.ZERO;

  @Column(nullable = false)
  private BigDecimal platformCommission = BigDecimal.ZERO;

  private String promoId;

  @Column(nullable = false)
  private String paymentStatus = "requires_payment";

  private String stripePaymentIntentId;

  @Column(nullable = false)
  private String status = "placed";

  @CreationTimestamp
  @Column(nullable = false)
  private OffsetDateTime placedAt;

  @CreationTimestamp
  @Column(updatable = false)
  private OffsetDateTime createdAt;

  @UpdateTimestamp
  private OffsetDateTime updatedAt;

  @PrePersist
  void pre() {
    if (id == null) id = IdGenerator.of("ord");
  }
}
