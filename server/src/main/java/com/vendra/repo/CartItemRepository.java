package com.vendra.repo;

import com.vendra.domain.CartItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CartItemRepository extends JpaRepository<CartItem, String> {
  List<CartItem> findByCartId(String cartId);

  void deleteByCartId(String cartId);
}
