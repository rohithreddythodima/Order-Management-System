package com.restaurant.management.dto;

import com.restaurant.management.entity.RoleName;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record StaffRequest(
        @NotBlank @Size(min = 3, max = 100) String username,
        @Size(max = 72) String password,
        @Size(max = 150) String fullName,
        @NotEmpty Set<RoleName> roles
) {
}
