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
@Table(name = "payouts")
@Getter
@Setter
@NoArgsConstructor
public class Payout {
  @Id private String id;

  @Column(nullable = false)
  private String payeeType;

  private String shopId;
  private String courierId;
  private String subOrderId;
  private String deliveryId;

  @Column(nullable = false)
  private BigDecimal amount;

  @Column(nullable = false)
  private String currency = "usd";

  @Column(nullable = false)
  private String status = "pending";

  private String stripeTransferId;

  @CreationTimestamp
  @Column(updatable = false)
  private OffsetDateTime createdAt;

  @UpdateTimestamp
  private OffsetDateTime updatedAt;

  @PrePersist
  void pre() {
    if (id == null) id = IdGenerator.of("pay");
  }
}
