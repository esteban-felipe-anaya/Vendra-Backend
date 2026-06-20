package com.vendra.repo;

import com.vendra.domain.ProductVariant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, String> {
  List<ProductVariant> findByProductId(String productId);

  /** Atomic conditional decrement; returns 1 if stock was sufficient, 0 otherwise. */
  @Modifying
  @Query(
      "update ProductVariant v set v.stock = v.stock - :qty "
          + "where v.id = :id and v.stock >= :qty")
  int decrementStock(@Param("id") String id, @Param("qty") int qty);
}
