package com.restaurant.management.controller;

import com.restaurant.management.dto.AuthRequest;
import com.restaurant.management.dto.AuthResponse;
import com.restaurant.management.dto.KitchenUserRequest;
import com.restaurant.management.dto.UserDto;
import com.restaurant.management.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final AuthenticationManager authenticationManager;

    @PostMapping("/setup-admin")
    public ResponseEntity<AuthResponse> setupAdmin(@Valid @RequestBody AuthRequest request) {
        UserDto user = authService.registerFirstAdmin(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new AuthResponse("Initial administrator created successfully", user));
    }

    @PostMapping("/register-admin")
    public ResponseEntity<AuthResponse> registerAdmin(@Valid @RequestBody AuthRequest request) {
        UserDto user = authService.registerAdmin(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new AuthResponse("Administrator account created successfully", user));
    }

    @PostMapping("/register-kitchen")
    public ResponseEntity<AuthResponse> registerKitchen(@Valid @RequestBody KitchenUserRequest request) {
        UserDto user = authService.registerKitchenUser(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new AuthResponse("Kitchen staff account created successfully", user));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest request,
                                            HttpServletRequest httpRequest) {
        try {
            UserDto user = authService.login(request);
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername().trim(), request.getPassword())
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);
            httpRequest.getSession(true).setAttribute("SPRING_SECURITY_CONTEXT", SecurityContextHolder.getContext());
            return ResponseEntity.ok(new AuthResponse("Login successful", user));
        } catch (BadCredentialsException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new AuthResponse("Invalid username or password", null));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<UserDto> me(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(authService.getUserByUsername(authentication.getName()));
    }

}
