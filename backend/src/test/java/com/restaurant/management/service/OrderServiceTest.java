package com.restaurant.management.service;

import com.restaurant.management.dto.OrderItemRequest;
import com.restaurant.management.dto.OrderItemsRequest;
import com.restaurant.management.dto.OrderRequest;
import com.restaurant.management.dto.OrderTrackingDto;
import com.restaurant.management.dto.OrderStatusRequest;
import com.restaurant.management.entity.CustomerOrder;
import com.restaurant.management.entity.MenuItem;
import com.restaurant.management.entity.OrderLineItem;
import com.restaurant.management.entity.OrderStatus;
import com.restaurant.management.entity.RestaurantTable;
import com.restaurant.management.entity.Role;
import com.restaurant.management.entity.RoleName;
import com.restaurant.management.entity.TableStatus;
import com.restaurant.management.entity.User;
import com.restaurant.management.repository.CustomerOrderRepository;
import com.restaurant.management.repository.MenuItemRepository;
import com.restaurant.management.repository.RestaurantTableRepository;
import com.restaurant.management.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {
    @Mock
    private CustomerOrderRepository orderRepository;
    @Mock
    private MenuItemRepository menuItemRepository;
    @Mock
    private RestaurantTableRepository tableRepository;
    @Mock
    private UserRepository userRepository;
    @InjectMocks
    private OrderService orderService;

    @Test
    void customerOrderGetsAnUnpredictableTrackingCodeAndServerCalculatedTotal() {
        MenuItem menuItem = new MenuItem();
        menuItem.setId(8L);
        menuItem.setName("Poutine");
        menuItem.setPrice(new BigDecimal("12.50"));
        menuItem.setAvailable(true);
        when(menuItemRepository.findById(8L)).thenReturn(Optional.of(menuItem));
        when(orderRepository.save(any(CustomerOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = orderService.create(new OrderRequest("Guest", List.of(new OrderItemRequest(8L, 2))));

        ArgumentCaptor<CustomerOrder> savedOrder = ArgumentCaptor.forClass(CustomerOrder.class);
        verify(orderRepository).save(savedOrder.capture());
        assertEquals(new BigDecimal("25.00"), result.total());
        assertNotNull(result.trackingCode());
        assertDoesNotThrow(() -> UUID.fromString(result.trackingCode()));
        assertEquals(result.trackingCode(), savedOrder.getValue().getTrackingCode());
        assertEquals(OrderStatus.NEW, savedOrder.getValue().getStatus());
    }

    @Test
    void trackingUsesTheSecretCodeAndReturnsOrderStatus() {
        CustomerOrder order = new CustomerOrder();
        order.setId(21L);
        order.setCustomerName("Guest");
        order.setStatus(OrderStatus.PREPARING);
        order.setTotal(new BigDecimal("12.50"));
        order.setTrackingCode("private-tracking-code");
        when(orderRepository.findByTrackingCode("private-tracking-code")).thenReturn(Optional.of(order));

        OrderTrackingDto result = orderService.track("private-tracking-code");

        assertEquals(21L, result.id());
        assertEquals(OrderStatus.PREPARING, result.status());
        verify(orderRepository).findByTrackingCode("private-tracking-code");
    }

    @Test
    void waiterOrderIsAssignedAndOccupiesItsTable() {
        User waiter = new User("server", "encoded", "Server");
        waiter.setRoles(Set.of(new Role(RoleName.WAITER)));
        RestaurantTable table = new RestaurantTable();
        table.setId(3L);
        table.setStatus(TableStatus.AVAILABLE);
        MenuItem menuItem = new MenuItem();
        menuItem.setId(8L);
        menuItem.setName("Poutine");
        menuItem.setPrice(new BigDecimal("12.50"));
        menuItem.setAvailable(true);
        when(userRepository.findByUsername("server")).thenReturn(Optional.of(waiter));
        when(tableRepository.findById(3L)).thenReturn(Optional.of(table));
        when(menuItemRepository.findById(8L)).thenReturn(Optional.of(menuItem));
        when(orderRepository.save(any(CustomerOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = orderService.create(
                new OrderRequest("Guest", List.of(new OrderItemRequest(8L, 1)), 3L), "server");

        assertEquals(3L, result.tableId());
        assertEquals("server", result.waiterUsername());
        assertEquals(TableStatus.OCCUPIED, table.getStatus());
        verify(tableRepository).save(table);
    }

    @Test
    void waiterCanAppendItemsToAnActiveOrderAndRequeueIt() {
        User waiter = new User("server", "encoded", "Server");
        RestaurantTable table = new RestaurantTable();
        table.setId(3L);
        table.setStatus(TableStatus.OCCUPIED);
        CustomerOrder order = new CustomerOrder();
        order.setId(25L);
        order.setCustomerName("Guest");
        order.setStatus(OrderStatus.READY);
        order.setTotal(new BigDecimal("10.00"));
        order.setRestaurantTable(table);
        order.setCreatedBy(waiter);
        order.setCreatedByUsername("server");
        MenuItem menuItem = new MenuItem();
        menuItem.setId(8L);
        menuItem.setName("Poutine");
        menuItem.setPrice(new BigDecimal("5.00"));
        menuItem.setAvailable(true);
        OrderLineItem existing = new OrderLineItem();
        existing.setOrder(order);
        existing.setMenuItemId(8L);
        existing.setItemName("Poutine");
        existing.setUnitPrice(new BigDecimal("5.00"));
        existing.setQuantity(2);
        existing.setLineTotal(new BigDecimal("10.00"));
        order.getItems().add(existing);
        when(orderRepository.findById(25L)).thenReturn(Optional.of(order));
        when(menuItemRepository.findById(8L)).thenReturn(Optional.of(menuItem));
        when(orderRepository.save(any(CustomerOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = orderService.addItems(25L,
                new OrderItemsRequest(List.of(new OrderItemRequest(8L, 1))), "server");

        assertEquals(new BigDecimal("15.00"), result.total());
        assertEquals(OrderStatus.NEW, result.status());
        assertEquals(3, result.items().get(0).quantity());
    }

    @Test
    void completedOrderCannotBeReopened() {
        CustomerOrder order = new CustomerOrder();
        order.setId(25L);
        order.setStatus(OrderStatus.COMPLETED);
        when(orderRepository.findById(25L)).thenReturn(Optional.of(order));

        assertThrows(ResponseStatusException.class,
                () -> orderService.updateStatus(25L, new OrderStatusRequest(OrderStatus.NEW)));
        verify(orderRepository, never()).save(any(CustomerOrder.class));
    }
}
