package com.restaurant.management.dto;

import java.math.BigDecimal;

public record MenuItemDto(
        Long id,
        String name,
        String description,
        String category,
        BigDecimal price,
        String imageUrl,
        boolean available
) {
}
