package com.restaurant.management.service;

import com.restaurant.management.dto.OrderItemRequest;
import com.restaurant.management.dto.OrderRequest;
import com.restaurant.management.dto.OrderTrackingDto;
import com.restaurant.management.entity.CustomerOrder;
import com.restaurant.management.entity.MenuItem;
import com.restaurant.management.entity.OrderStatus;
import com.restaurant.management.repository.CustomerOrderRepository;
import com.restaurant.management.repository.MenuItemRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
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
}
