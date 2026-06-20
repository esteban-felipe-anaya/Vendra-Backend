package com.vendra.domain;

import com.vendra.common.IdGenerator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "sub_orders")
@Getter
@Setter
@NoArgsConstructor
public class SubOrder {
  @Id private String id;

  @Column(nullable = false)
  private String orderId;

  @Column(nullable = false)
  private String shopId;

  @Column(nullable = false)
  private String status = "placed";

  @Column(nullable = false)
  private BigDecimal itemsSubtotal = BigDecimal.ZERO;

  @Column(nullable = false)
  private BigDecimal commissionRate = new BigDecimal("0.1000");

  @Column(nullable = false)
  private BigDecimal commissionAmount = BigDecimal.ZERO;

  @Column(nullable = false)
  private BigDecimal shopPayout = BigDecimal.ZERO;

  @Column(nullable = false)
  private String payoutStatus = "pending";

  private String stripeTransferId;
  private OffsetDateTime acceptedAt;
  private OffsetDateTime readyAt;
  private OffsetDateTime deliveredAt;

  @CreationTimestamp
  @Column(updatable = false)
  private OffsetDateTime createdAt;

  @UpdateTimestamp
  private OffsetDateTime updatedAt;

  @PrePersist
  void pre() {
    if (id == null) id = IdGenerator.of("sub");
  }
}
