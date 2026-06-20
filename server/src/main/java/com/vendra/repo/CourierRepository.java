package com.vendra.repo;

import com.vendra.domain.Courier;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourierRepository extends JpaRepository<Courier, String> {
  Optional<Courier> findByProfileId(UUID profileId);

  List<Courier> findByAvailability(String availability);
}
