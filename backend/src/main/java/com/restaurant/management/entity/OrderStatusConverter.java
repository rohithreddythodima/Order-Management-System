package com.restaurant.management.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class OrderStatusConverter implements AttributeConverter<OrderStatus, String> {
    // Existing databases and demo data store the terminal order status as COMPLETED.
    @Override
    public String convertToDatabaseColumn(OrderStatus status) {
        return status == null ? null : status == OrderStatus.SERVED ? "COMPLETED" : status.name();
    }

    @Override
    public OrderStatus convertToEntityAttribute(String value) {
        return value == null ? null : "COMPLETED".equals(value) ? OrderStatus.SERVED : OrderStatus.valueOf(value);
    }
}
