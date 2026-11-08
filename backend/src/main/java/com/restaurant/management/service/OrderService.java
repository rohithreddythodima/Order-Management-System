package com.restaurant.management.service;

import com.restaurant.management.dto.OrderDto;
import com.restaurant.management.dto.OrderItemRequest;
import com.restaurant.management.dto.OrderLineItemDto;
import com.restaurant.management.dto.OrderRequest;
import com.restaurant.management.dto.OrderStatusRequest;
import com.restaurant.management.dto.OrderTrackingDto;
import com.restaurant.management.entity.CustomerOrder;
import com.restaurant.management.entity.MenuItem;
import com.restaurant.management.entity.OrderLineItem;
import com.restaurant.management.repository.CustomerOrderRepository;
import com.restaurant.management.repository.MenuItemRepository;
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

    @Transactional(readOnly = true)
    public List<OrderDto> getOrders() {
        return orderRepository.findAllByOrderByCreatedAtDesc().stream().map(this::toDto).toList();
    }

    @Transactional
    public OrderDto create(OrderRequest request) {
        CustomerOrder order = new CustomerOrder();
        String customerName = request.customerName() == null ? "" : request.customerName().trim();
        order.setCustomerName(customerName.isBlank() ? "Walk-in customer" : customerName);
        order.setTrackingCode(UUID.randomUUID().toString());

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
        return toDto(orderRepository.save(order), true);
    }

    @Transactional
    public OrderDto updateStatus(long id, OrderStatusRequest request) {
        CustomerOrder order = findById(id);
        order.setStatus(request.status());
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
        orderRepository.delete(findById(id));
    }

    private CustomerOrder findById(long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
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
                includeTrackingCode ? order.getTrackingCode() : null
        );
    }
}
