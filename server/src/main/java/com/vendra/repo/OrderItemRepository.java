package com.vendra.repo;

import com.vendra.domain.OrderItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository extends JpaRepository<OrderItem, String> {
  List<OrderItem> findBySubOrderId(String subOrderId);
}
