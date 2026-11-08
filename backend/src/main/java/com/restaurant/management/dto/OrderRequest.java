package com.restaurant.management.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record OrderRequest(
        @Size(max = 120) String customerName,
        @NotEmpty @Valid List<OrderItemRequest> items
) {
}
