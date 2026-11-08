package com.restaurant.management.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record OrderItemRequest(
        @NotNull @Positive Long menuItemId,
        @Positive int quantity
) {
}
