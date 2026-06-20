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
@Table(name = "addresses")
@Getter
@Setter
@NoArgsConstructor
public class Address {
  @Id private String id;

  @Column(nullable = false)
  private UUID userId;

  private String label;

  @Column(nullable = false)
  private String recipient;

  private String phone;

  @Column(nullable = false)
  private String line1;

  private String line2;

  @Column(nullable = false)
  private String city;

  private String region;
  private String postalCode;

  @Column(nullable = false)
  private String country = "US";

  private BigDecimal lat;
  private BigDecimal lng;
  private boolean isDefault = false;

  @CreationTimestamp
  @Column(updatable = false)
  private OffsetDateTime createdAt;

  @UpdateTimestamp
  private OffsetDateTime updatedAt;

  @PrePersist
  void pre() {
    if (id == null) id = IdGenerator.of("adr");
  }
}
