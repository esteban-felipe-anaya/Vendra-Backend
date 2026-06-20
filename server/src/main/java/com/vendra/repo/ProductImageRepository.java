package com.vendra.repo;

import com.vendra.domain.ProductImage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductImageRepository extends JpaRepository<ProductImage, String> {
  List<ProductImage> findByProductIdOrderBySortOrderAsc(String productId);
}
