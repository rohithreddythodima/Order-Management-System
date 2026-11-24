package com.restaurant.management.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record OrderItemsRequest(@NotEmpty @Valid List<OrderItemRequest> items) {
}
