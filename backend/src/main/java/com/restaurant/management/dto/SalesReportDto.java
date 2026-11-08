package com.restaurant.management.dto;

import java.math.BigDecimal;
import java.util.List;

public record SalesReportDto(
        long ordersToday,
        BigDecimal revenueToday,
        long ordersThisMonth,
        BigDecimal revenueThisMonth,
        List<DailySalesDto> dailySales
) {
}
