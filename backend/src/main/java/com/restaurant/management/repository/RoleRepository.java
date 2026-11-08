package com.restaurant.management.repository;

import com.restaurant.management.entity.Role;
import com.restaurant.management.entity.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(RoleName name);
}
