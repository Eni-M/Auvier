package com.auvier.dtos.user;

import com.auvier.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for returning user data to clients.
 *
 * Why this structure:
 * - NEVER includes password (security critical!)
 * - Excludes orders list (fetched separately via OrderController to avoid huge payloads)
 * - Includes role for frontend authorization decisions
 * - Includes createdAt for profile display
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponseDto {

    private Long id;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String address;
    private String phone;
    private Role role;
    private LocalDateTime createdAt;

    // Computed/derived field - useful for display
    public String getFullName() {
        if (firstName == null && lastName == null) return null;
        if (firstName == null) return lastName;
        if (lastName == null) return firstName;
        return firstName + " " + lastName;
    }
}

