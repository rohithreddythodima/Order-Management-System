package com.restaurant.management.controller;

import com.restaurant.management.dto.SalesReportDto;
import com.restaurant.management.service.SalesReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class SalesReportController {
    private final SalesReportService salesReportService;

    @GetMapping("/sales")
    @PreAuthorize("hasAnyRole('ADMIN', 'CASHIER')")
    public SalesReportDto getSalesReport() {
        return salesReportService.getReport();
    }
}
