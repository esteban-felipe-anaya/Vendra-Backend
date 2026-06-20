package com.vendra.repo;

import com.vendra.domain.WishlistItem;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WishlistItemRepository extends JpaRepository<WishlistItem, String> {
  List<WishlistItem> findByUserId(UUID userId);

  Optional<WishlistItem> findByUserIdAndProductId(UUID userId, String productId);
}
