package com.restaurant.management.controller;

import com.restaurant.management.dto.KitchenUserRequest;
import com.restaurant.management.dto.UserDto;
import com.restaurant.management.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminUserController {
    private final AuthService authService;

    @PostMapping("/kitchen-users")
    @ResponseStatus(HttpStatus.CREATED)
    public UserDto createKitchenUser(@Valid @RequestBody KitchenUserRequest request) {
        return authService.createKitchenUser(request);
    }
}
