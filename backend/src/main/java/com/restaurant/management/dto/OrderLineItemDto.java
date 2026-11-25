package com.restaurant.management.dto;

import com.restaurant.management.entity.OrderItemStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderLineItemDto(
        Long id,
        Long menuItemId,
        String itemName,
        BigDecimal unitPrice,
        int quantity,
        BigDecimal lineTotal,
        OrderItemStatus status,
        LocalDateTime createdAt
) {
}
