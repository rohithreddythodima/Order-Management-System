package com.restaurant.management.service;

import com.restaurant.management.dto.StaffRequest;
import com.restaurant.management.dto.UserDto;
import com.restaurant.management.entity.OrderStatus;
import com.restaurant.management.entity.Role;
import com.restaurant.management.entity.RoleName;
import com.restaurant.management.entity.User;
import com.restaurant.management.repository.CustomerOrderRepository;
import com.restaurant.management.repository.RoleRepository;
import com.restaurant.management.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StaffService {
    private static final List<OrderStatus> ACTIVE_STATUSES =
            List.of(OrderStatus.NEW, OrderStatus.PREPARING, OrderStatus.READY);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final CustomerOrderRepository orderRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public List<UserDto> getStaff() {
        return userRepository.findAll().stream()
                .filter(user -> user.getRoles().stream().noneMatch(role -> role.getName() == RoleName.ADMIN))
                .sorted(Comparator.comparing(User::getUsername, String.CASE_INSENSITIVE_ORDER))
                .map(this::toDto).toList();
    }

    @Transactional
    public UserDto create(StaffRequest request) {
        String username = request.username().trim();
        if (userRepository.existsByUsername(username)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "That username is already in use");
        }
        validatePassword(request.password(), true);
        User user = new User(username, passwordEncoder.encode(request.password()), normalizedName(request.fullName(), username));
        user.setRoles(resolveRoles(request.roles()));
        user.setEnabled(true);
        return toDto(userRepository.save(user));
    }

    @Transactional
    public UserDto update(long id, StaffRequest request) {
        User user = findById(id);
        ensureStaffAccount(user);
        String username = request.username().trim();
        if (userRepository.existsByUsernameAndIdNot(username, id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "That username is already in use");
        }
        user.setUsername(username);
        user.setFullName(normalizedName(request.fullName(), username));
        user.setRoles(resolveRoles(request.roles()));
        if (request.password() != null && !request.password().isBlank()) {
            validatePassword(request.password(), true);
            user.setPassword(passwordEncoder.encode(request.password()));
            user.setFailedLoginAttempts(0);
            user.setLockedUntil(null);
            user.setEnabled(true);
        }
        return toDto(userRepository.save(user));
    }

    @Transactional
    public void delete(long id, String actingUsername) {
        User user = findById(id);
        ensureStaffAccount(user);
        if (user.getUsername().equalsIgnoreCase(actingUsername)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You cannot delete your active profile");
        }
        if (orderRepository.existsByCreatedBy_IdAndStatusIn(user.getId(), ACTIVE_STATUSES)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Staff accounts linked to active orders cannot be deleted");
        }
        orderRepository.clearStaffReferences(user.getId());
        userRepository.deleteRoleAssignments(user.getId());
        userRepository.delete(user);
    }

    private Set<Role> resolveRoles(Set<RoleName> requested) {
        if (requested == null || requested.isEmpty() || requested.contains(RoleName.ADMIN)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Staff must have at least one non-administrator role");
        }
        return requested.stream().map(name -> roleRepository.findByName(name)
                        .orElseGet(() -> roleRepository.save(new Role(name))))
                .collect(Collectors.toSet());
    }

    private void validatePassword(String password, boolean required) {
        if (password == null || password.isBlank()) {
            if (required) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password is required");
            return;
        }
        int bytes = password.getBytes(StandardCharsets.UTF_8).length;
        if (password.length() < 12 || bytes > 72) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Password must be at least 12 characters and no more than 72 UTF-8 bytes");
        }
    }

    private User findById(long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Staff account not found"));
    }

    private void ensureStaffAccount(User user) {
        if (user.getRoles().stream().anyMatch(role -> role.getName() == RoleName.ADMIN)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Administrator profiles cannot be managed as staff");
        }
    }

    private String normalizedName(String fullName, String username) {
        return fullName == null || fullName.isBlank() ? username : fullName.trim();
    }

    private UserDto toDto(User user) {
        return new UserDto(user.getId(), user.getUsername(), user.getFullName(),
                user.getRoles().stream().map(role -> role.getName().name()).sorted().toList(),
                user.isEnabled(), user.getCreatedAt());
    }
}
