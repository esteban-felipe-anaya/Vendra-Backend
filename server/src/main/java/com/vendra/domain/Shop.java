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
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "shops")
@Getter
@Setter
@NoArgsConstructor
public class Shop {
  @Id private String id;

  @Column(nullable = false)
  private UUID ownerId;

  @Column(nullable = false)
  private String name;

  @Column(nullable = false)
  private String slug;

  private String description;
  private String logoUrl;
  private String bannerUrl;

  @Column(nullable = false)
  private String status = "pending"; // pending | approved | suspended

  private String stripeAccountId;
  private boolean payoutsEnabled = false;
  private BigDecimal ratingAvg = BigDecimal.ZERO;
  private int ratingCount = 0;

  @CreationTimestamp
  @Column(updatable = false)
  private OffsetDateTime createdAt;

  @UpdateTimestamp
  private OffsetDateTime updatedAt;

  @PrePersist
  void pre() {
    if (id == null) id = IdGenerator.of("shop");
  }
}
