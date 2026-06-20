package com.vendra.repo;

import com.vendra.domain.Delivery;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DeliveryRepository extends JpaRepository<Delivery, String> {
  Optional<Delivery> findBySubOrderId(String subOrderId);

  List<Delivery> findByCourierId(String courierId);

  List<Delivery> findByStatus(String status);

  Optional<Delivery> findByOrderId(String orderId);

  /**
   * Atomic first-accept-wins claim: assign the courier only if the offer is still unclaimed.
   * Returns 1 if this courier won the offer, 0 if someone already took it.
   */
  @Modifying
  @Query(
      "update Delivery d set d.courierId = :courierId, d.status = 'assigned', "
          + "d.assignedAt = :now where d.id = :id and d.status = 'offered' and d.courierId is null")
  int claim(
      @Param("id") String id,
      @Param("courierId") String courierId,
      @Param("now") OffsetDateTime now);
}
