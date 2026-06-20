package com.vendra.repo;

import com.vendra.domain.Cart;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CartRepository extends JpaRepository<Cart, String> {
  Optional<Cart> findByUserId(UUID userId);
}
