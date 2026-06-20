package com.vendra.repo;

import com.vendra.domain.Address;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AddressRepository extends JpaRepository<Address, String> {
  List<Address> findByUserId(UUID userId);
}
