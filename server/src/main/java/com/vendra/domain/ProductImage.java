package com.vendra.domain;

import com.vendra.common.IdGenerator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "product_images")
@Getter
@Setter
@NoArgsConstructor
public class ProductImage {
  @Id private String id;

  @Column(nullable = false)
  private String productId;

  @Column(nullable = false)
  private String url;

  private int sortOrder = 0;

  @CreationTimestamp
  @Column(updatable = false)
  private OffsetDateTime createdAt;

  @PrePersist
  void pre() {
    if (id == null) id = IdGenerator.of("img");
  }
}
