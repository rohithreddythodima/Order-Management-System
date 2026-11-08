package com.restaurant.management.service;

import com.restaurant.management.dto.DailySalesDto;
import com.restaurant.management.dto.SalesReportDto;
import com.restaurant.management.entity.CustomerOrder;
import com.restaurant.management.repository.CustomerOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SalesReportService {
    private final CustomerOrderRepository orderRepository;

    @Transactional(readOnly = true)
    public SalesReportDto getReport() {
        LocalDate today = LocalDate.now();
        LocalDateTime monthStart = YearMonth.from(today).atDay(1).atStartOfDay();
        LocalDateTime dayStart = today.atStartOfDay();
        LocalDate firstDay = today.minusDays(29);
        List<CustomerOrder> orders = orderRepository.findByCreatedAtGreaterThanEqualOrderByCreatedAtAsc(
                firstDay.atStartOfDay());

        List<CustomerOrder> todayOrders = orders.stream()
                .filter(order -> !order.getCreatedAt().isBefore(dayStart))
                .toList();
        List<CustomerOrder> monthOrders = orders.stream()
                .filter(order -> !order.getCreatedAt().isBefore(monthStart))
                .toList();
        Map<LocalDate, List<CustomerOrder>> byDay = orders.stream()
                .collect(Collectors.groupingBy(order -> order.getCreatedAt().toLocalDate()));

        List<DailySalesDto> dailySales = new ArrayList<>();
        for (LocalDate day = firstDay; !day.isAfter(today); day = day.plusDays(1)) {
            List<CustomerOrder> dayOrders = byDay.getOrDefault(day, List.of());
            dailySales.add(new DailySalesDto(day, dayOrders.size(), sum(dayOrders)));
        }

        return new SalesReportDto(
                todayOrders.size(),
                sum(todayOrders),
                monthOrders.size(),
                sum(monthOrders),
                dailySales
        );
    }

    private BigDecimal sum(List<CustomerOrder> orders) {
        return orders.stream()
                .map(CustomerOrder::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
