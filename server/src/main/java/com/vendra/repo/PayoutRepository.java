package com.vendra.repo;

import com.vendra.domain.Payout;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PayoutRepository extends JpaRepository<Payout, String> {
  List<Payout> findByShopId(String shopId);

  List<Payout> findByCourierId(String courierId);
}
