package com.restaurant.management.service;

import com.restaurant.management.dto.OrderDto;
import com.restaurant.management.dto.OrderItemRequest;
import com.restaurant.management.dto.OrderItemsRequest;
import com.restaurant.management.dto.OrderLineItemDto;
import com.restaurant.management.dto.OrderRequest;
import com.restaurant.management.dto.OrderTrackingDto;
import com.restaurant.management.entity.CustomerOrder;
import com.restaurant.management.entity.MenuItem;
import com.restaurant.management.entity.OrderLineItem;
import com.restaurant.management.entity.OrderItemStatus;
import com.restaurant.management.entity.OrderStatus;
import com.restaurant.management.entity.RestaurantTable;
import com.restaurant.management.entity.RoleName;
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
import java.time.LocalDateTime;
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

    @Transactional(readOnly = true)
    public List<OrderDto> getOrdersForWaiter(String username) {
        return orderRepository.findOrdersForWaiter(username).stream().map(this::toDto).toList();
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
                .anyMatch(role -> role.getName() == RoleName.WAITER);
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
            RestaurantTable table = tableRepository.findForUpdate(request.tableId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Table not found"));
            if (table.getStatus() != TableStatus.AVAILABLE) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Only available tables can start a new order");
            }
            order.setRestaurantTable(table);
            order.setTableNumber(table.getId());
        }

        BigDecimal total = BigDecimal.ZERO;
        LocalDateTime addedAt = LocalDateTime.now();
        for (OrderItemRequest requestedItem : request.items()) {
            MenuItem menuItem = findAvailableMenuItem(requestedItem.menuItemId());
            total = total.add(addLine(order, menuItem, requestedItem.quantity(), addedAt));
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
        CustomerOrder order = findForUpdate(id);
        if (order.getRestaurantTable() == null || !isActive(order.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Items can only be added to an active table order");
        }
        if (!isAssignedTo(order, actorUsername)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only add items to your own table orders");
        }

        initializeItemStatuses(order);
        LocalDateTime addedAt = LocalDateTime.now();
        BigDecimal addedTotal = BigDecimal.ZERO;
        for (OrderItemRequest requestedItem : request.items()) {
            MenuItem menuItem = findAvailableMenuItem(requestedItem.menuItemId());
            addedTotal = addedTotal.add(addLine(order, menuItem, requestedItem.quantity(), addedAt));
        }

        order.setTotal(order.getTotal().add(addedTotal));
        order.setStatus(OrderStatus.NEW);
        return toDto(orderRepository.saveAndFlush(order));
    }

    @Transactional
    public OrderDto updateItemStatus(long orderId, long itemId, OrderItemStatus next, String actorUsername) {
        CustomerOrder order = findForUpdate(orderId);
        boolean pickup = order.getRestaurantTable() == null && order.getTableNumber() == null;
        if (next == OrderItemStatus.SERVED && !pickup && !isAssignedTo(order, actorUsername)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only serve items at your assigned tables");
        }
        OrderLineItem item = order.getItems().stream()
                .filter(line -> Long.valueOf(itemId).equals(line.getId()))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order item not found"));

        OrderItemStatus current = itemStatus(order, item);
        if (current == next) {
            return toDto(order);
        }
        boolean allowed = switch (current) {
            case NEW -> next == OrderItemStatus.PREPARING;
            case PREPARING -> next == OrderItemStatus.READY;
            case READY -> next == OrderItemStatus.SERVED;
            case SERVED -> false;
        };
        if (!allowed) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Invalid item status transition from " + current + " to " + next);
        }

        initializeItemStatuses(order);
        item.setStatus(next);
        // Enum order follows kitchen progress, so the least advanced item determines the order status.
        OrderItemStatus earliest = order.getItems().stream()
                .map(OrderLineItem::getStatus)
                .min(OrderItemStatus::compareTo)
                .orElseThrow();
        order.setStatus(OrderStatus.valueOf(earliest.name()));
        if (order.getStatus() == OrderStatus.SERVED && order.getRestaurantTable() != null) {
            order.getRestaurantTable().setStatus(TableStatus.AVAILABLE);
            tableRepository.save(order.getRestaurantTable());
        }
        return toDto(order);
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
        CustomerOrder order = findForUpdate(id);
        if (order.getRestaurantTable() != null && isActive(order.getStatus())) {
            order.getRestaurantTable().setStatus(TableStatus.AVAILABLE);
            tableRepository.save(order.getRestaurantTable());
        }
        orderRepository.delete(order);
    }

    private CustomerOrder findForUpdate(long id) {
        return orderRepository.findForUpdate(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
    }

    private boolean isActive(OrderStatus status) {
        return status == OrderStatus.NEW || status == OrderStatus.PREPARING || status == OrderStatus.READY;
    }

    private boolean isAssignedTo(CustomerOrder order, String username) {
        return order.getCreatedBy() != null && order.getCreatedBy().getUsername().equals(username);
    }

    private MenuItem findAvailableMenuItem(long id) {
        MenuItem item = menuItemRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "A selected menu item no longer exists"));
        if (!item.isAvailable()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, item.getName() + " is not available");
        }
        return item;
    }

    private BigDecimal addLine(CustomerOrder order, MenuItem menuItem, int quantity, LocalDateTime addedAt) {
        OrderLineItem line = new OrderLineItem();
        line.setOrder(order);
        line.setMenuItemId(menuItem.getId());
        line.setItemName(menuItem.getName());
        line.setUnitPrice(menuItem.getPrice());
        line.setQuantity(quantity);
        line.setLineTotal(menuItem.getPrice().multiply(BigDecimal.valueOf(quantity)));
        line.setCreatedAt(addedAt);
        line.setStatus(OrderItemStatus.NEW);
        order.getItems().add(line);
        return line.getLineTotal();
    }

    private OrderItemStatus itemStatus(CustomerOrder order, OrderLineItem item) {
        return item.getStatus() == null ? OrderItemStatus.valueOf(order.getStatus().name()) : item.getStatus();
    }

    private void initializeItemStatuses(CustomerOrder order) {
        for (OrderLineItem item : order.getItems()) {
            if (item.getStatus() == null) {
                item.setStatus(itemStatus(order, item));
            }
        }
    }

    private OrderDto toDto(CustomerOrder order) {
        return toDto(order, false);
    }

    private OrderDto toDto(CustomerOrder order, boolean includeTrackingCode) {
        List<OrderLineItemDto> items = order.getItems().stream()
                .map(item -> new OrderLineItemDto(
                        item.getId(),
                        item.getMenuItemId(),
                        item.getItemName(),
                        item.getUnitPrice(),
                        item.getQuantity(),
                        item.getLineTotal(),
                        itemStatus(order, item),
                        item.getCreatedAt() == null ? order.getCreatedAt() : item.getCreatedAt()
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
