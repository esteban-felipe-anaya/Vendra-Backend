package com.vendra.repo;

import com.vendra.domain.SubOrder;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubOrderRepository extends JpaRepository<SubOrder, String> {
  List<SubOrder> findByOrderId(String orderId);

  List<SubOrder> findByShopIdOrderByCreatedAtDesc(String shopId);

  List<SubOrder> findByStatus(String status);
}
