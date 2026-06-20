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
@Table(name = "deliveries")
@Getter
@Setter
@NoArgsConstructor
public class Delivery {
  @Id private String id;

  @Column(nullable = false)
  private String subOrderId;

  @Column(nullable = false)
  private String orderId;

  private String courierId;

  @Column(nullable = false)
  private String status = "offered";

  private BigDecimal pickupLat;
  private BigDecimal pickupLng;
  private String pickupLabel;
  private BigDecimal dropoffLat;
  private BigDecimal dropoffLng;
  private String dropoffLabel;

  @Column(nullable = false)
  private BigDecimal courierFee = BigDecimal.ZERO;

  @Column(nullable = false)
  private String payoutStatus = "pending";

  private String stripeTransferId;

  @CreationTimestamp
  @Column(nullable = false)
  private OffsetDateTime offeredAt;

  private OffsetDateTime assignedAt;
  private OffsetDateTime pickedUpAt;
  private OffsetDateTime deliveredAt;

  @CreationTimestamp
  @Column(updatable = false)
  private OffsetDateTime createdAt;

  @UpdateTimestamp
  private OffsetDateTime updatedAt;

  @PrePersist
  void pre() {
    if (id == null) id = IdGenerator.of("del");
  }
}
