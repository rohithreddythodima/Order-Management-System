package com.restaurant.management.service;

import com.restaurant.management.dto.RestaurantTableDto;
import com.restaurant.management.dto.TableRequest;
import com.restaurant.management.entity.OrderStatus;
import com.restaurant.management.entity.RestaurantTable;
import com.restaurant.management.repository.CustomerOrderRepository;
import com.restaurant.management.repository.RestaurantTableRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RestaurantTableService {
    private static final List<OrderStatus> ACTIVE_STATUSES =
            List.of(OrderStatus.NEW, OrderStatus.PREPARING, OrderStatus.READY);

    private final RestaurantTableRepository tableRepository;
    private final CustomerOrderRepository orderRepository;

    @Transactional(readOnly = true)
    public List<RestaurantTableDto> getTables() {
        return tableRepository.findAll().stream().sorted((a, b) -> a.getId().compareTo(b.getId()))
                .map(this::toDto).toList();
    }

    @Transactional
    public RestaurantTableDto create(TableRequest request) {
        if (tableRepository.count() == 0 && request.id() != 1L) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The first table number must be 1");
        }
        if (tableRepository.existsById(request.id())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "That table number already exists");
        }
        RestaurantTable table = new RestaurantTable();
        table.setId(request.id());
        table.setStatus(request.status());
        return toDto(tableRepository.save(table));
    }

    @Transactional
    public RestaurantTableDto update(long id, TableRequest request) {
        if (!request.id().equals(id)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The table number cannot be changed");
        }
        RestaurantTable table = findById(id);
        boolean hasActiveOrder = !orderRepository.findActiveOrdersForTable(id, ACTIVE_STATUSES).isEmpty();
        if (hasActiveOrder && request.status() != com.restaurant.management.entity.TableStatus.OCCUPIED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "A table with an active order must remain occupied");
        }
        table.setStatus(request.status());
        return toDto(tableRepository.save(table));
    }

    @Transactional
    public void delete(long id) {
        RestaurantTable table = findById(id);
        if (!orderRepository.findActiveOrdersForTable(id, ACTIVE_STATUSES).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Tables linked to active orders cannot be deleted");
        }
        orderRepository.clearTableReferences(id);
        tableRepository.delete(table);
    }

    private RestaurantTable findById(long id) {
        return tableRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Table not found"));
    }

    private RestaurantTableDto toDto(RestaurantTable table) {
        Long activeOrderId = orderRepository.findActiveOrdersForTable(table.getId(), ACTIVE_STATUSES).stream()
                .findFirst().map(order -> order.getId()).orElse(null);
        return new RestaurantTableDto(table.getId(), table.getStatus(), activeOrderId);
    }
}
