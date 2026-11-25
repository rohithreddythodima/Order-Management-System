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
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${order-desk.auth.admin-self-signup:false}")
    private boolean adminSelfSignup;

    @Transactional
    public UserDto registerFirstAdmin(AuthRequest request) {
        if (!isInitialSetupRequired()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Initial administrator setup is only available when no user profiles exist");
        }
        return createAdmin(request);
    }

    @Transactional
    public UserDto registerAdmin(AuthRequest request) {
        return registerFirstAdmin(request);
    }

    @Transactional
    public UserDto registerAdditionalAdmin(AuthRequest request) {
        return createAdmin(request);
    }

    @Transactional
    public UserDto registerStaffAccount(AuthRequest request, RoleName roleName) {
        if (roleName == RoleName.ADMIN && !adminSelfSignup) {
            return registerFirstAdmin(request);
        }
        if (roleName != RoleName.ADMIN && roleName != RoleName.KITCHEN
                && roleName != RoleName.WAITER && roleName != RoleName.CASHIER) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Self-registration is not available for this role");
        }
        String username = validateSignup(request);
        Role role = roleRepository.findByName(roleName)
                .orElseGet(() -> roleRepository.save(new Role(roleName)));
        String fullName = request.getFullName() == null || request.getFullName().isBlank()
                ? username : request.getFullName().trim();
        User user = new User(username, passwordEncoder.encode(request.getPassword()), fullName);
        user.setRoles(Set.of(role));
        user.setEnabled(true);
        return mapToDto(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public boolean isInitialSetupRequired() {
        return userRepository.count() == 0;
    }

    private UserDto createAdmin(AuthRequest request) {
        String username = validateSignup(request);
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

    private String validateSignup(AuthRequest request) {
        String username = request.getUsername() == null ? "" : request.getUsername().trim();
        if (username.length() < 3) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username must be at least 3 characters");
        }

        String password = request.getPassword();
        if (password == null || password.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password is required");
        }
        if (password.length() < 12 || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Password must be at least 12 characters and at most 72 UTF-8 bytes");
        }
        if (!password.equals(request.getConfirmPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Passwords do not match");
        }
        if (userRepository.existsByUsername(username)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "That username is already in use");
        }
        return username;
    }

    @Transactional(noRollbackFor = BadCredentialsException.class)
    public UserDto login(AuthRequest request) {
        User user = userRepository.findByUsername(request.getUsername().trim())
                .orElseThrow(() -> new BadCredentialsException("Invalid username or password"));

        LocalDateTime now = LocalDateTime.now();
        if (user.getLockedUntil() != null) {
            if (user.getLockedUntil().isAfter(now)) {
                throw new BadCredentialsException("Account locked. Try again in 15 minutes.");
            }
            user.setLockedUntil(null);
            user.setFailedLoginAttempts(0);
            user.setEnabled(true);
        }
        if (!user.isEnabled()) {
            throw new BadCredentialsException("Account is disabled");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            int failedAttempts = user.getFailedLoginAttempts() + 1;
            user.setFailedLoginAttempts(failedAttempts);
            if (failedAttempts >= 5) {
                user.setLockedUntil(now.plusMinutes(15));
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
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
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
