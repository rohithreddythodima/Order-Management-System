package com.restaurant.management.controller;

import com.restaurant.management.dto.OrderDto;
import com.restaurant.management.dto.OrderRequest;
import com.restaurant.management.dto.OrderItemsRequest;
import com.restaurant.management.dto.OrderItemStatusRequest;
import com.restaurant.management.entity.OrderItemStatus;
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
import org.springframework.web.bind.annotation.RequestParam;
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
    public List<OrderDto> getOrders(@RequestParam(defaultValue = "false") boolean assignedTables,
                                   Authentication authentication) {
        boolean waiter = hasRole(authentication, "WAITER");
        if (assignedTables && !waiter) {
            throw new AccessDeniedException("Only waiters can access the table orders view");
        }
        boolean canViewAll = hasRole(authentication, "ADMIN") || hasRole(authentication, "CASHIER")
                || hasRole(authentication, "KITCHEN") || hasRole(authentication, "CHEF");
        return assignedTables || !canViewAll
                ? orderService.getOrdersForWaiter(authentication.getName())
                : orderService.getOrders();
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

    @PatchMapping("/{id}/items/{itemId}/status")
    @PreAuthorize("hasAnyRole('KITCHEN', 'CHEF', 'WAITER')")
    public OrderDto updateItemStatus(@PathVariable long id, @PathVariable long itemId,
                                     @Valid @RequestBody OrderItemStatusRequest request,
                                     Authentication authentication) {
        OrderItemStatus status = request.status();
        boolean permitted = switch (status) {
            case PREPARING, READY -> hasRole(authentication, "KITCHEN") || hasRole(authentication, "CHEF");
            case SERVED -> hasRole(authentication, "WAITER");
            case NEW -> false;
        };
        if (!permitted) {
            throw new AccessDeniedException("This role cannot set an item to that status");
        }
        return orderService.updateItemStatus(id, itemId, status, authentication.getName());
    }

    private boolean hasRole(Authentication authentication, String role) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_" + role));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable long id) {
        orderService.delete(id);
    }
}
