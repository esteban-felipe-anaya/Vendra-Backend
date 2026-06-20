package com.vendra.repo;

import com.vendra.domain.Promo;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PromoRepository extends JpaRepository<Promo, String> {
  Optional<Promo> findByCode(String code);
}
