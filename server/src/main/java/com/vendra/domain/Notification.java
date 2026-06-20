package com.vendra.domain;

import com.vendra.common.IdGenerator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
public class Notification {
  @Id private String id;

  @Column(nullable = false)
  private UUID userId;

  @Column(nullable = false)
  private String type;

  @Column(nullable = false)
  private String title;

  private String body;
  private String refType;
  private String refId;
  private boolean isRead = false;

  @CreationTimestamp
  @Column(updatable = false)
  private OffsetDateTime createdAt;

  @PrePersist
  void pre() {
    if (id == null) id = IdGenerator.of("ntf");
  }
}
