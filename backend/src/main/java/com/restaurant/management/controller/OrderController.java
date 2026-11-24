package com.restaurant.management.controller;

import com.restaurant.management.dto.OrderDto;
import com.restaurant.management.dto.OrderRequest;
import com.restaurant.management.dto.OrderItemsRequest;
import com.restaurant.management.dto.OrderStatusRequest;
import com.restaurant.management.dto.OrderTrackingDto;
import com.restaurant.management.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orderService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'KITCHEN', 'CHEF', 'WAITER', 'CASHIER')")
    public List<OrderDto> getOrders() {
        return orderService.getOrders();
    }

    @GetMapping("/track/{trackingCode}")
    public OrderTrackingDto track(@PathVariable String trackingCode) {
        return orderService.track(trackingCode);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderDto create(@Valid @RequestBody OrderRequest request, Authentication authentication) {
        String username = authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getName()) ? null : authentication.getName();
        return orderService.create(request, username);
    }

    @PostMapping("/{id}/items")
    @PreAuthorize("hasRole('WAITER')")
    public OrderDto addItems(@PathVariable long id, @Valid @RequestBody OrderItemsRequest request,
                             Authentication authentication) {
        return orderService.addItems(id, request, authentication.getName());
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'KITCHEN', 'CHEF', 'CASHIER')")
    public OrderDto updateStatus(@PathVariable long id, @Valid @RequestBody OrderStatusRequest request,
                                 Authentication authentication) {
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
        boolean isKitchen = authentication.getAuthorities().stream()
            .anyMatch(authority -> authority.getAuthority().equals("ROLE_KITCHEN")
                || authority.getAuthority().equals("ROLE_CHEF"));
        boolean isCashier = authentication.getAuthorities().stream()
            .anyMatch(authority -> authority.getAuthority().equals("ROLE_CASHIER"));
        boolean kitchenTransitionAllowed = isKitchen
                && (request.status() == com.restaurant.management.entity.OrderStatus.PREPARING
                || request.status() == com.restaurant.management.entity.OrderStatus.READY);
        boolean cashierTransitionAllowed = isCashier
                && request.status() == com.restaurant.management.entity.OrderStatus.COMPLETED;
        if (!isAdmin && !kitchenTransitionAllowed && !cashierTransitionAllowed) {
            throw new AccessDeniedException("This role cannot set the order to that status");
        }
        return orderService.updateStatus(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable long id) {
        orderService.delete(id);
    }
}
