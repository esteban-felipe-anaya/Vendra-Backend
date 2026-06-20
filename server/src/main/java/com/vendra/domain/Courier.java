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
@Table(name = "couriers")
@Getter
@Setter
@NoArgsConstructor
public class Courier {
  @Id private String id;

  @Column(nullable = false)
  private UUID profileId;

  @Column(nullable = false)
  private String vehicleType = "bike"; // bike | scooter | car | foot

  @Column(nullable = false)
  private String availability = "offline"; // offline | available | busy

  private BigDecimal currentLat;
  private BigDecimal currentLng;
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
    if (id == null) id = IdGenerator.of("cou");
  }
}
