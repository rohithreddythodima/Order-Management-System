package com.restaurant.management.service;

import com.restaurant.management.dto.OrderDto;
import com.restaurant.management.dto.OrderItemRequest;
import com.restaurant.management.dto.OrderItemsRequest;
import com.restaurant.management.dto.OrderLineItemDto;
import com.restaurant.management.dto.OrderRequest;
import com.restaurant.management.dto.OrderStatusRequest;
import com.restaurant.management.dto.OrderTrackingDto;
import com.restaurant.management.entity.CustomerOrder;
import com.restaurant.management.entity.MenuItem;
import com.restaurant.management.entity.OrderLineItem;
import com.restaurant.management.entity.OrderStatus;
import com.restaurant.management.entity.RestaurantTable;
import com.restaurant.management.entity.TableStatus;
import com.restaurant.management.entity.User;
import com.restaurant.management.repository.CustomerOrderRepository;
import com.restaurant.management.repository.MenuItemRepository;
import com.restaurant.management.repository.RestaurantTableRepository;
import com.restaurant.management.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderService {
    private final CustomerOrderRepository orderRepository;
    private final MenuItemRepository menuItemRepository;
    private final RestaurantTableRepository tableRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<OrderDto> getOrders() {
        return orderRepository.findAllByOrderByCreatedAtDesc().stream().map(this::toDto).toList();
    }

    @Transactional
    public OrderDto create(OrderRequest request) {
        return create(request, null);
    }

    @Transactional
    public OrderDto create(OrderRequest request, String actorUsername) {
        CustomerOrder order = new CustomerOrder();
        String customerName = request.customerName() == null ? "" : request.customerName().trim();
        order.setCustomerName(customerName.isBlank() ? "Walk-in customer" : customerName);
        order.setTrackingCode(UUID.randomUUID().toString());

        User actor = actorUsername == null ? null : userRepository.findByUsername(actorUsername)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sign in to place this order"));
        boolean waiter = actor != null && actor.getRoles().stream()
                .anyMatch(role -> role.getName() == com.restaurant.management.entity.RoleName.WAITER);
        if (waiter && request.tableId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Waiter orders must be assigned to a table");
        }
        if (request.tableId() != null && !waiter) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only waiters can place table orders");
        }
        if (waiter) {
            order.setCreatedBy(actor);
            order.setCreatedByUsername(actor.getUsername());
        }
        if (request.tableId() != null) {
            RestaurantTable table = tableRepository.findById(request.tableId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Table not found"));
            if (table.getStatus() != TableStatus.AVAILABLE) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Only available tables can start a new order");
            }
            order.setRestaurantTable(table);
            order.setTableNumber(table.getId());
        }

        BigDecimal total = BigDecimal.ZERO;
        for (OrderItemRequest requestedItem : request.items()) {
            MenuItem menuItem = menuItemRepository.findById(requestedItem.menuItemId())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.BAD_REQUEST, "A selected menu item no longer exists"));
            if (!menuItem.isAvailable()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, menuItem.getName() + " is not available");
            }

            OrderLineItem line = new OrderLineItem();
            line.setOrder(order);
            line.setMenuItemId(menuItem.getId());
            line.setItemName(menuItem.getName());
            line.setUnitPrice(menuItem.getPrice());
            line.setQuantity(requestedItem.quantity());
            line.setLineTotal(menuItem.getPrice().multiply(BigDecimal.valueOf(requestedItem.quantity())));
            total = total.add(line.getLineTotal());
            order.getItems().add(line);
        }

        order.setTotal(total);
        CustomerOrder saved = orderRepository.save(order);
        if (saved.getRestaurantTable() != null) {
            saved.getRestaurantTable().setStatus(TableStatus.OCCUPIED);
            tableRepository.save(saved.getRestaurantTable());
        }
        return toDto(saved, true);
    }

    @Transactional
    public OrderDto addItems(long id, OrderItemsRequest request, String actorUsername) {
        CustomerOrder order = findById(id);
        if (order.getRestaurantTable() == null || !isActive(order.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Items can only be added to an active table order");
        }
        if (order.getCreatedBy() == null || !order.getCreatedBy().getUsername().equals(actorUsername)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only add items to your own table orders");
        }

        BigDecimal addedTotal = BigDecimal.ZERO;
        for (OrderItemRequest requestedItem : request.items()) {
            MenuItem menuItem = menuItemRepository.findById(requestedItem.menuItemId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "A selected menu item no longer exists"));
            if (!menuItem.isAvailable()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, menuItem.getName() + " is not available");
            }
            OrderLineItem existing = order.getItems().stream()
                    .filter(item -> menuItem.getId().equals(item.getMenuItemId()))
                    .findFirst().orElse(null);
            if (existing == null) {
                existing = new OrderLineItem();
                existing.setOrder(order);
                existing.setMenuItemId(menuItem.getId());
                existing.setItemName(menuItem.getName());
                existing.setUnitPrice(menuItem.getPrice());
                existing.setQuantity(0);
                existing.setLineTotal(BigDecimal.ZERO);
                order.getItems().add(existing);
            }
            existing.setQuantity(existing.getQuantity() + requestedItem.quantity());
            existing.setLineTotal(existing.getUnitPrice().multiply(BigDecimal.valueOf(existing.getQuantity())));
            addedTotal = addedTotal.add(menuItem.getPrice().multiply(BigDecimal.valueOf(requestedItem.quantity())));
        }
        order.setTotal(order.getTotal().add(addedTotal));
        order.setStatus(OrderStatus.NEW);
        return toDto(orderRepository.save(order));
    }

    @Transactional
    public OrderDto updateStatus(long id, OrderStatusRequest request) {
        CustomerOrder order = findById(id);
        if (!isAllowedTransition(order.getStatus(), request.status())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Invalid order status transition from " + order.getStatus() + " to " + request.status());
        }
        order.setStatus(request.status());
        if (request.status() == OrderStatus.COMPLETED && order.getRestaurantTable() != null) {
            order.getRestaurantTable().setStatus(TableStatus.AVAILABLE);
            tableRepository.save(order.getRestaurantTable());
        }
        return toDto(orderRepository.save(order));
    }

    @Transactional(readOnly = true)
    public OrderTrackingDto track(String trackingCode) {
        CustomerOrder order = orderRepository.findByTrackingCode(trackingCode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        OrderDto dto = toDto(order);
        return new OrderTrackingDto(dto.id(), dto.customerName(), dto.status(), dto.total(),
                dto.createdAt(), dto.items());
    }

    @Transactional
    public void delete(long id) {
        CustomerOrder order = findById(id);
        if (order.getRestaurantTable() != null && isActive(order.getStatus())) {
            order.getRestaurantTable().setStatus(TableStatus.AVAILABLE);
            tableRepository.save(order.getRestaurantTable());
        }
        orderRepository.delete(order);
    }

    private CustomerOrder findById(long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
    }

    private boolean isActive(OrderStatus status) {
        return status == OrderStatus.NEW || status == OrderStatus.PREPARING || status == OrderStatus.READY;
    }

    private boolean isAllowedTransition(OrderStatus current, OrderStatus next) {
        if (current == next) return true;
        return (current == OrderStatus.NEW && next == OrderStatus.PREPARING)
                || (current == OrderStatus.PREPARING && next == OrderStatus.READY)
                || (current == OrderStatus.READY && next == OrderStatus.COMPLETED);
    }

    private OrderDto toDto(CustomerOrder order) {
        return toDto(order, false);
    }

    private OrderDto toDto(CustomerOrder order, boolean includeTrackingCode) {
        List<OrderLineItemDto> items = order.getItems().stream()
                .map(item -> new OrderLineItemDto(
                        item.getMenuItemId(),
                        item.getItemName(),
                        item.getUnitPrice(),
                        item.getQuantity(),
                        item.getLineTotal()
                ))
                .toList();

        return new OrderDto(
                order.getId(),
                order.getCustomerName(),
                order.getStatus(),
                order.getTotal(),
                order.getCreatedAt(),
                items,
                includeTrackingCode ? order.getTrackingCode() : null,
                order.getRestaurantTable() == null ? order.getTableNumber() : order.getRestaurantTable().getId(),
                order.getCreatedBy() == null ? order.getCreatedByUsername() : order.getCreatedBy().getUsername()
        );
    }
}
