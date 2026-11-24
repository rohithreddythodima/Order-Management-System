package com.restaurant.management.dto;

import com.restaurant.management.entity.TableStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record TableRequest(@NotNull @Positive Long id, @NotNull TableStatus status) {
}
