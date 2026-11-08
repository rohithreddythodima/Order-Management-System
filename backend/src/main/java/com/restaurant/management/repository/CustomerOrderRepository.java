package com.restaurant.management.repository;

import com.restaurant.management.entity.CustomerOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface CustomerOrderRepository extends JpaRepository<CustomerOrder, Long> {
    List<CustomerOrder> findAllByOrderByCreatedAtDesc();

    java.util.Optional<CustomerOrder> findByTrackingCode(String trackingCode);

    List<CustomerOrder> findByCreatedAtGreaterThanEqualOrderByCreatedAtAsc(LocalDateTime createdAt);
}
