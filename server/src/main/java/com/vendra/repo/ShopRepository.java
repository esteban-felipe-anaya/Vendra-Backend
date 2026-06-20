package com.vendra.repo;

import com.vendra.domain.Shop;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShopRepository extends JpaRepository<Shop, String> {
  List<Shop> findByOwnerId(UUID ownerId);

  List<Shop> findByStatus(String status);

  Optional<Shop> findBySlug(String slug);
}
