package com.vendra.repo;

import com.vendra.domain.Ledger;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LedgerRepository extends JpaRepository<Ledger, String> {
  List<Ledger> findByOrderId(String orderId);
}
