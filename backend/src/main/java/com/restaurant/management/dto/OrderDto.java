package com.restaurant.management.dto;

import com.restaurant.management.entity.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderDto(
        Long id,
        String customerName,
        OrderStatus status,
        BigDecimal total,
        LocalDateTime createdAt,
        List<OrderLineItemDto> items,
        String trackingCode
) {
}
