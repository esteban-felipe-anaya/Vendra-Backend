package com.vendra.repo;

import com.vendra.domain.CourierLocation;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourierLocationRepository extends JpaRepository<CourierLocation, String> {
  List<CourierLocation> findByDeliveryIdOrderByRecordedAtDesc(String deliveryId);
}
