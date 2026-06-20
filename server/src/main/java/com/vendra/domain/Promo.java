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
@Table(name = "promos")
@Getter
@Setter
@NoArgsConstructor
public class Promo {
  @Id private String id;

  @Column(nullable = false)
  private String code;

  private String description;

  @Column(nullable = false)
  private String discountType;

  @Column(nullable = false)
  private BigDecimal amount;

  @Column(nullable = false)
  private BigDecimal minSubtotal = BigDecimal.ZERO;

  private Integer maxRedemptions;

  @Column(nullable = false)
  private int redeemedCount = 0;

  private OffsetDateTime startsAt;
  private OffsetDateTime endsAt;
  private boolean isActive = true;

  @CreationTimestamp
  @Column(updatable = false)
  private OffsetDateTime createdAt;

  @UpdateTimestamp
  private OffsetDateTime updatedAt;

  @PrePersist
  void pre() {
    if (id == null) id = IdGenerator.of("promo");
  }
}
