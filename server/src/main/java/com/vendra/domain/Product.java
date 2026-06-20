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
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
public class Product {
  @Id private String id;

  @Column(nullable = false)
  private String shopId;

  private String categoryId;

  @Column(nullable = false)
  private String name;

  @Column(nullable = false)
  private String slug;

  private String description;

  @Column(nullable = false)
  private BigDecimal price;

  @Column(nullable = false)
  private String currency = "usd";

  @Column(nullable = false)
  private int stock = 0;

  private String imageUrl;
  private boolean isActive = true;
  private BigDecimal ratingAvg = BigDecimal.ZERO;
  private int ratingCount = 0;
  private int soldCount = 0;

  @CreationTimestamp
  @Column(updatable = false)
  private OffsetDateTime createdAt;

  @UpdateTimestamp
  private OffsetDateTime updatedAt;

  @PrePersist
  void pre() {
    if (id == null) id = IdGenerator.of("prd");
  }
}
