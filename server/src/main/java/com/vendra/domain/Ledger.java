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
@Table(name = "ledger")
@Getter
@Setter
@NoArgsConstructor
public class Ledger {
  @Id private String id;

  private String orderId;
  private String subOrderId;

  @Column(nullable = false)
  private String entryType;

  @Column(nullable = false)
  private BigDecimal amount;

  @Column(nullable = false)
  private String currency = "usd";

  private String memo;

  @CreationTimestamp
  @Column(updatable = false)
  private OffsetDateTime createdAt;

  @PrePersist
  void pre() {
    if (id == null) id = IdGenerator.of("led");
  }
}
