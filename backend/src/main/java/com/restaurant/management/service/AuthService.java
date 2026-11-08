package com.restaurant.management.service;

import com.restaurant.management.dto.AuthRequest;
import com.restaurant.management.dto.KitchenUserRequest;
import com.restaurant.management.dto.UserDto;
import com.restaurant.management.entity.Role;
import com.restaurant.management.entity.RoleName;
import com.restaurant.management.entity.User;
import com.restaurant.management.repository.RoleRepository;
import com.restaurant.management.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserDto registerFirstAdmin(AuthRequest request) {
        if (userRepository.count() > 0) {
            throw new IllegalStateException("Initial admin already exists");
        }
        return createAdmin(request);
    }

    @Transactional
    public UserDto registerAdmin(AuthRequest request) {
        return createAdmin(request);
    }

    private UserDto createAdmin(AuthRequest request) {
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new IllegalArgumentException("Password is required");
        }
        if (request.getPassword().length() < 12
                || request.getPassword().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("Administrator password must be at least 12 characters and at most 72 UTF-8 bytes");
        }
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match");
        }
        String username = request.getUsername().trim();
        if (userRepository.existsByUsername(username)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "That username is already in use");
        }

        Role adminRole = roleRepository.findByName(RoleName.ADMIN)
                .orElseGet(() -> roleRepository.save(new Role(RoleName.ADMIN)));

        User user = new User(
                username,
                passwordEncoder.encode(request.getPassword()),
                request.getFullName() != null ? request.getFullName().trim() : "System Administrator"
        );
        user.setRoles(Set.of(adminRole));
        user.setEnabled(true);
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);

        User saved = userRepository.save(user);
        return mapToDto(saved);
    }

    public UserDto login(AuthRequest request) {
        User user = userRepository.findByUsername(request.getUsername().trim())
                .orElseThrow(() -> new BadCredentialsException("Invalid username or password"));

        if (!user.isEnabled()) {
            throw new BadCredentialsException("Account is locked temporarily");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            int failedAttempts = user.getFailedLoginAttempts() + 1;
            user.setFailedLoginAttempts(failedAttempts);
            if (failedAttempts >= 5) {
                user.setEnabled(false);
                user.setLockedUntil(LocalDateTime.now().plusMinutes(15));
            }
            userRepository.save(user);
            throw new BadCredentialsException("Invalid password");
        }

        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        userRepository.save(user);

        return mapToDto(user);
    }

    @Transactional
    public UserDto createKitchenUser(KitchenUserRequest request) {
        return createKitchenUserAccount(request);
    }

    @Transactional
    public UserDto registerKitchenUser(KitchenUserRequest request) {
        return createKitchenUserAccount(request);
    }

    private UserDto createKitchenUserAccount(KitchenUserRequest request) {
        String username = request.username().trim();
        if (request.password().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must be at most 72 UTF-8 bytes");
        }
        if (userRepository.existsByUsername(username)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "That username is already in use");
        }

        Role kitchenRole = roleRepository.findByName(RoleName.KITCHEN)
                .orElseGet(() -> roleRepository.save(new Role(RoleName.KITCHEN)));
        User user = new User(username, passwordEncoder.encode(request.password()),
                request.fullName() == null || request.fullName().isBlank()
                        ? username : request.fullName().trim());
        user.setRoles(Set.of(kitchenRole));
        user.setEnabled(true);
        return mapToDto(userRepository.save(user));
    }

    public UserDto getUserByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BadCredentialsException("User not found"));
        return mapToDto(user);
    }

    private UserDto mapToDto(User user) {
        return new UserDto(
                user.getId(),
                user.getUsername(),
                user.getFullName(),
                user.getRoles().stream()
                        .map(role -> role.getName().name())
                        .sorted(Comparator.naturalOrder())
                        .toList(),
                user.isEnabled(),
                user.getCreatedAt()
        );
    }
}
