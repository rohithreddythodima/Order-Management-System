package com.restaurant.management.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class UserDto {
    private Long id;
    private String username;
    private String fullName;
    private List<String> roles;
    private boolean enabled;
    private LocalDateTime createdAt;
}
