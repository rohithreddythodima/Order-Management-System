package com.restaurant.management.service;

import com.restaurant.management.dto.MenuItemDto;
import com.restaurant.management.dto.MenuItemRequest;
import com.restaurant.management.entity.MenuItem;
import com.restaurant.management.repository.MenuItemRepository;
import com.restaurant.management.repository.CustomerOrderRepository;
import com.restaurant.management.entity.OrderStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MenuService {
    private final MenuItemRepository menuItemRepository;
    private final CustomerOrderRepository orderRepository;

    @Transactional(readOnly = true)
    public List<MenuItemDto> getMenu(String search) {
        var items = search == null || search.isBlank()
                ? menuItemRepository.findAllByOrderByNameAsc()
                : menuItemRepository.findByNameContainingIgnoreCaseOrCategoryContainingIgnoreCaseOrderByNameAsc(
                        search.trim(), search.trim());
        return items.stream().map(this::toDto).toList();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public MenuItemDto create(MenuItemRequest request) {
        MenuItem item = new MenuItem();
        apply(item, request);
        return toDto(menuItemRepository.save(item));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public MenuItemDto update(long id, MenuItemRequest request) {
        MenuItem item = findById(id);
        apply(item, request);
        return toDto(menuItemRepository.save(item));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public void delete(long id) {
        MenuItem item = findById(id);
        if (orderRepository.existsActiveOrderForMenuItem(item.getId(),
                List.of(OrderStatus.NEW, OrderStatus.PREPARING, OrderStatus.READY))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Menu items linked to active orders cannot be deleted");
        }
        menuItemRepository.delete(item);
    }

    private MenuItem findById(long id) {
        return menuItemRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Menu item not found"));
    }

    private void apply(MenuItem item, MenuItemRequest request) {
        item.setName(request.name().trim());
        item.setDescription(request.description() == null ? "" : request.description().trim());
        item.setCategory(request.category() == null || request.category().isBlank()
            ? null : request.category().trim());
        item.setPrice(request.price().setScale(2, java.math.RoundingMode.HALF_UP));
        item.setImageUrl(request.imageUrl());
        item.setAvailable(request.available() == null || request.available());
    }

    private MenuItemDto toDto(MenuItem item) {
        return new MenuItemDto(
                item.getId(),
                item.getName(),
                item.getDescription(),
                item.getCategory(),
                item.getPrice(),
                item.getImageUrl(),
                item.isAvailable()
        );
    }
}
