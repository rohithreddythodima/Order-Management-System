package com.restaurant.management.repository;

import com.restaurant.management.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);
    boolean existsByUsernameAndIdNot(String username, Long id);

    @Modifying
    @Query(value = "delete from user_roles where user_id = :userId", nativeQuery = true)
    int deleteRoleAssignments(@Param("userId") Long userId);
}
