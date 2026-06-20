package com.vendra.repo;

import com.vendra.domain.Notification;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, String> {
  List<Notification> findByUserIdOrderByCreatedAtDesc(UUID userId);

  long countByUserIdAndIsReadFalse(UUID userId);
}
