package com.restaurant.management.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class WorkspaceController {

    @GetMapping("/api/admin/dashboard")
    public ResponseEntity<Map<String, String>> adminDashboard() {
        return ResponseEntity.ok(Map.of("workspace", "admin", "status", "ready"));
    }

    @GetMapping("/api/waiter/dashboard")
    public ResponseEntity<Map<String, String>> waiterDashboard() {
        return ResponseEntity.ok(Map.of("workspace", "waiter", "status", "ready"));
    }

    @GetMapping("/api/kitchen/dashboard")
    public ResponseEntity<Map<String, String>> kitchenDashboard() {
        return ResponseEntity.ok(Map.of("workspace", "kitchen", "status", "ready"));
    }

    @GetMapping("/api/cashier/dashboard")
    public ResponseEntity<Map<String, String>> cashierDashboard() {
        return ResponseEntity.ok(Map.of("workspace", "cashier", "status", "ready"));
    }
}
