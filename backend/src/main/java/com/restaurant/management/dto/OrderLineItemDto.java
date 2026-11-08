package com.restaurant.management.dto;

import java.math.BigDecimal;

public record OrderLineItemDto(
        Long menuItemId,
        String itemName,
        BigDecimal unitPrice,
        int quantity,
        BigDecimal lineTotal
) {
}
