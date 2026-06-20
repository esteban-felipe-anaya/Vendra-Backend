package com.vendra.repo;

import com.vendra.domain.Product;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, String> {
  List<Product> findByShopId(String shopId);

  List<Product> findByCategoryId(String categoryId);

  List<Product> findByIsActiveTrue();

  Optional<Product> findByShopIdAndSlug(String shopId, String slug);

  /** Atomic conditional decrement; returns 1 if stock was sufficient, 0 otherwise. */
  @Modifying
  @Query(
      "update Product p set p.stock = p.stock - :qty, p.soldCount = p.soldCount + :qty "
          + "where p.id = :id and p.stock >= :qty")
  int decrementStock(@Param("id") String id, @Param("qty") int qty);
}
