package com.vendra.repo;

import com.vendra.domain.Review;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRepository extends JpaRepository<Review, String> {
  List<Review> findByProductId(String productId);

  List<Review> findByUserId(UUID userId);
}
