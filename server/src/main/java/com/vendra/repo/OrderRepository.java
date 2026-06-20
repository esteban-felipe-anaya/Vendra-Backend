package com.vendra.repo;

import com.vendra.domain.Order;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, String> {
  List<Order> findByCustomerIdOrderByCreatedAtDesc(UUID customerId);
}
