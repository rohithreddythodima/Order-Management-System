package com.restaurant.management.dto;

import com.restaurant.management.entity.TableStatus;

public record RestaurantTableDto(Long id, TableStatus status, Long activeOrderId) {
}
