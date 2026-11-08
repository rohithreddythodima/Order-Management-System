package com.restaurant.management.service;

import com.restaurant.management.dto.KitchenUserRequest;
import com.restaurant.management.dto.AuthRequest;
import com.restaurant.management.entity.Role;
import com.restaurant.management.entity.RoleName;
import com.restaurant.management.entity.User;
import com.restaurant.management.repository.RoleRepository;
import com.restaurant.management.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

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

        var result = serviceWithEncoder.createKitchenUser(
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
}
