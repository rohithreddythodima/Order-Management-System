package com.restaurant.management.service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import com.restaurant.management.dto.AuthRequest;
import com.restaurant.management.dto.KitchenUserRequest;
import com.restaurant.management.entity.Role;
import com.restaurant.management.entity.RoleName;
import com.restaurant.management.entity.User;
import com.restaurant.management.repository.RoleRepository;
import com.restaurant.management.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Test
    void provisioningCreatesEnabledKitchenAccountWithHashedPassword() {
        when(userRepository.existsByUsername("line-cook")).thenReturn(false);
        when(roleRepository.findByName(RoleName.KITCHEN)).thenReturn(Optional.of(new Role(RoleName.KITCHEN)));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        AuthService serviceWithEncoder = new AuthService(
                userRepository, roleRepository, new BCryptPasswordEncoder());

        var result = serviceWithEncoder.registerKitchenUser(
                new KitchenUserRequest(" line-cook ", "a-very-long-password", "Line Cook"));

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User saved = userCaptor.getValue();
        assertEquals("line-cook", result.getUsername());
        assertTrue(saved.isEnabled());
        assertTrue(saved.getRoles().stream().anyMatch(role -> role.getName() == RoleName.KITCHEN));
        assertNotEquals("a-very-long-password", saved.getPassword());
        assertTrue(new BCryptPasswordEncoder().matches("a-very-long-password", saved.getPassword()));
    }

    @Test
    void registrationCreatesEnabledAdminAccountWithHashedPassword() {
        when(userRepository.existsByUsername("manager")).thenReturn(false);
        when(roleRepository.findByName(RoleName.ADMIN)).thenReturn(Optional.of(new Role(RoleName.ADMIN)));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        AuthService serviceWithEncoder = new AuthService(
                userRepository, roleRepository, new BCryptPasswordEncoder());
        AuthRequest request = new AuthRequest();
        request.setUsername(" manager ");
        request.setPassword("a-very-long-password");
        request.setConfirmPassword("a-very-long-password");
        request.setFullName("Restaurant Manager");

        var result = serviceWithEncoder.registerAdmin(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User saved = userCaptor.getValue();
        assertEquals("manager", result.getUsername());
        assertTrue(saved.isEnabled());
        assertTrue(saved.getRoles().stream().anyMatch(role -> role.getName() == RoleName.ADMIN));
        assertTrue(new BCryptPasswordEncoder().matches("a-very-long-password", saved.getPassword()));
    }

    @Test
    void staffSelfRegistrationAllowsKitchenWaiterAndCashier() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        AuthService serviceWithEncoder = new AuthService(userRepository, roleRepository, encoder);
        for (RoleName roleName : Set.of(RoleName.KITCHEN, RoleName.WAITER, RoleName.CASHIER)) {
            String username = roleName.name().toLowerCase();
            AuthRequest request = new AuthRequest();
            request.setUsername(username);
            request.setPassword("a-very-long-password");
            request.setConfirmPassword("a-very-long-password");
            when(userRepository.existsByUsername(username)).thenReturn(false);
            when(roleRepository.findByName(roleName)).thenReturn(Optional.of(new Role(roleName)));
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

            var result = serviceWithEncoder.registerStaffAccount(request, roleName);

            assertEquals(username, result.getUsername());
            assertTrue(result.getRoles().contains(roleName.name()));
        }
    }

    @Test
    void administratorSelfSignupRequiresAnEmptyDatabaseByDefault() {
        when(userRepository.count()).thenReturn(1L);
        AuthService serviceWithEncoder = new AuthService(
                userRepository, roleRepository, new BCryptPasswordEncoder());

        var error = assertThrows(ResponseStatusException.class,
                () -> serviceWithEncoder.registerStaffAccount(new AuthRequest(), RoleName.ADMIN));

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void initialAdminCannotBeRegisteredWhenProfilesAlreadyExist() {
        when(userRepository.count()).thenReturn(1L);
        AuthService serviceWithEncoder = new AuthService(
                userRepository, roleRepository, new BCryptPasswordEncoder());

        var error = assertThrows(ResponseStatusException.class,
                () -> serviceWithEncoder.registerFirstAdmin(new AuthRequest()));

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void staffSignupRejectsUsernameThatIsTooShortAfterTrimming() {
        AuthService serviceWithEncoder = new AuthService(
                userRepository, roleRepository, new BCryptPasswordEncoder());
        AuthRequest request = new AuthRequest();
        request.setUsername(" ab ");
        request.setPassword("a-very-long-password");
        request.setConfirmPassword("a-very-long-password");

        var error = assertThrows(ResponseStatusException.class,
                () -> serviceWithEncoder.registerStaffAccount(request, RoleName.KITCHEN));

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        assertEquals("Username must be at least 3 characters", error.getReason());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void fifthInvalidPasswordStartsTemporaryLockout() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        User user = new User("line-cook", encoder.encode("correct-password"), "Line Cook");
        user.setRoles(Set.of(new Role(RoleName.KITCHEN)));
        when(userRepository.findByUsername("line-cook")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        AuthService serviceWithEncoder = new AuthService(userRepository, roleRepository, encoder);
        AuthRequest invalidRequest = new AuthRequest();
        invalidRequest.setUsername("line-cook");
        invalidRequest.setPassword("incorrect-password");

        for (int attempt = 0; attempt < 5; attempt++) {
            assertThrows(BadCredentialsException.class, () -> serviceWithEncoder.login(invalidRequest));
        }

        assertEquals(5, user.getFailedLoginAttempts());
        assertNotNull(user.getLockedUntil());
        assertTrue(user.getLockedUntil().isAfter(LocalDateTime.now()));
        assertTrue(user.isEnabled());
        assertThrows(BadCredentialsException.class, () -> serviceWithEncoder.login(invalidRequest));
    }

    @Test
    void expiredLockoutIsClearedAndSuccessfulLoginResetsAttemptCount() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        User user = new User("line-cook", encoder.encode("correct-password"), "Line Cook");
        user.setRoles(Set.of(new Role(RoleName.KITCHEN)));
        user.setEnabled(false);
        user.setFailedLoginAttempts(5);
        user.setLockedUntil(LocalDateTime.now().minusMinutes(1));
        when(userRepository.findByUsername("line-cook")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        AuthService serviceWithEncoder = new AuthService(userRepository, roleRepository, encoder);
        AuthRequest validRequest = new AuthRequest();
        validRequest.setUsername("line-cook");
        validRequest.setPassword("correct-password");

        var result = serviceWithEncoder.login(validRequest);

        assertEquals("line-cook", result.getUsername());
        assertTrue(user.isEnabled());
        assertEquals(0, user.getFailedLoginAttempts());
        assertNull(user.getLockedUntil());
    }
}
