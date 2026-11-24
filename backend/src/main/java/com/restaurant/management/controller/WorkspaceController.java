package com.restaurant.management.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.Map;

@RestController
public class WorkspaceController {

    @GetMapping("/api/admin/dashboard")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> adminDashboard() {
        return ResponseEntity.ok(Map.of("workspace", "admin", "status", "ready"));
    }

    @GetMapping("/api/waiter/dashboard")
    @PreAuthorize("hasRole('WAITER')")
    public ResponseEntity<Map<String, String>> waiterDashboard() {
        return ResponseEntity.ok(Map.of("workspace", "waiter", "status", "ready"));
    }

    @GetMapping("/api/kitchen/dashboard")
    @PreAuthorize("hasAnyRole('KITCHEN', 'CHEF')")
    public ResponseEntity<Map<String, String>> kitchenDashboard() {
        return ResponseEntity.ok(Map.of("workspace", "kitchen", "status", "ready"));
    }

    @GetMapping("/api/cashier/dashboard")
    @PreAuthorize("hasRole('CASHIER')")
    public ResponseEntity<Map<String, String>> cashierDashboard() {
        return ResponseEntity.ok(Map.of("workspace", "cashier", "status", "ready"));
    }
}
