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

@Entity
@Table(name = "order_items")
@Getter
@Setter
@NoArgsConstructor
public class OrderItem {
  @Id private String id;

  @Column(nullable = false)
  private String subOrderId;

  private String productId;
  private String variantId;

  @Column(nullable = false)
  private String nameSnapshot;

  private String imageSnapshot;

  @Column(nullable = false)
  private BigDecimal unitPrice;

  @Column(nullable = false)
  private int quantity;

  @Column(nullable = false)
  private BigDecimal lineTotal;

  @CreationTimestamp
  @Column(updatable = false)
  private OffsetDateTime createdAt;

  @PrePersist
  void pre() {
    if (id == null) id = IdGenerator.of("itm");
  }
}
