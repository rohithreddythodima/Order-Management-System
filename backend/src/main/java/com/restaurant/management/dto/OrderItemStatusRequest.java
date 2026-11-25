package com.restaurant.management.dto;

import com.restaurant.management.entity.OrderItemStatus;
import jakarta.validation.constraints.NotNull;

public record OrderItemStatusRequest(@NotNull OrderItemStatus status) {
}
