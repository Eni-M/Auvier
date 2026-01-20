package com.auvier.dtos.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for updating user profile.
 *
 * Why separate from registration:
 * - Username typically shouldn't change after registration
 * - Password update should be a separate flow with current password verification
 * - Email change might require re-verification
 * - All fields optional (partial updates allowed)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserUpdateDto {

    @Email(message = "Please provide a valid email address")
    private String email;

    @Size(max = 100)
    private String firstName;

    @Size(max = 100)
    private String lastName;

    @Size(max = 500)
    private String address;

    @Size(max = 20)
    private String phone;
}

