package com.restaurant.management.repository;

import com.restaurant.management.entity.CustomerOrder;
import com.restaurant.management.entity.OrderStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Collection;
import java.util.Optional;

public interface CustomerOrderRepository extends JpaRepository<CustomerOrder, Long> {
    List<CustomerOrder> findAllByOrderByCreatedAtDesc();

    @Query("""
            select o from CustomerOrder o left join o.createdBy waiter
            where waiter.username = :username or o.tableNumber is null and o.restaurantTable is null
            order by o.createdAt desc, o.id desc
            """)
    List<CustomerOrder> findOrdersForWaiter(@Param("username") String username);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from CustomerOrder o where o.id = :id")
    Optional<CustomerOrder> findForUpdate(@Param("id") long id);

    Optional<CustomerOrder> findByTrackingCode(String trackingCode);

    List<CustomerOrder> findByCreatedAtGreaterThanEqualOrderByCreatedAtAsc(LocalDateTime createdAt);

    @Query("select (count(i) > 0) from CustomerOrder o join o.items i " +
            "where i.menuItemId = :menuItemId and o.status in :statuses")
    boolean existsActiveOrderForMenuItem(@Param("menuItemId") Long menuItemId,
                                         @Param("statuses") Collection<OrderStatus> statuses);

    @Query("select o from CustomerOrder o where o.restaurantTable.id = :tableId and o.status in :statuses " +
            "order by o.createdAt desc")
    List<CustomerOrder> findActiveOrdersForTable(@Param("tableId") Long tableId,
                                                  @Param("statuses") Collection<OrderStatus> statuses);

    boolean existsByCreatedBy_IdAndStatusIn(Long userId, Collection<OrderStatus> statuses);

        @Modifying
        @Query(value = "update customer_orders set table_id = null where table_id = :tableId", nativeQuery = true)
        int clearTableReferences(@Param("tableId") Long tableId);

        @Modifying
        @Query(value = "update customer_orders set created_by_user_id = null where created_by_user_id = :userId", nativeQuery = true)
        int clearStaffReferences(@Param("userId") Long userId);
}
