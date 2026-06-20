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
@Table(name = "courier_locations")
@Getter
@Setter
@NoArgsConstructor
public class CourierLocation {
  @Id private String id;

  @Column(nullable = false)
  private String deliveryId;

  @Column(nullable = false)
  private String courierId;

  @Column(nullable = false)
  private BigDecimal lat;

  @Column(nullable = false)
  private BigDecimal lng;

  private BigDecimal heading;

  @CreationTimestamp
  @Column(nullable = false)
  private OffsetDateTime recordedAt;

  @PrePersist
  void pre() {
    if (id == null) id = IdGenerator.of("loc");
  }
}
