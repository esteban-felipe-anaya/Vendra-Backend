package com.vendra.notify;

import com.vendra.domain.Notification;
import com.vendra.repo.NotificationRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Persists notifications. The row insert is what Supabase Realtime broadcasts to the recipient's
 * subscription, so persisting IS the push — no separate channel needed.
 */
@Service
public class NotificationService {

  private final NotificationRepository repo;

  public NotificationService(NotificationRepository repo) {
    this.repo = repo;
  }

  public void notify(
      UUID userId, String type, String title, String body, String refType, String refId) {
    Notification n = new Notification();
    n.setUserId(userId);
    n.setType(type);
    n.setTitle(title);
    n.setBody(body);
    n.setRefType(refType);
    n.setRefId(refId);
    repo.save(n);
  }
}
