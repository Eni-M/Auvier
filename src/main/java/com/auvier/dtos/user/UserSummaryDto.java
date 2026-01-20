package com.auvier.dtos.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Lightweight DTO for embedding user info in other responses (e.g., Orders).
 *
 * Why needed:
 * - Order response needs to show who placed it, but not full user details
 * - Protects user privacy (no email, phone, address in order listings)
 * - Reduces payload size
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSummaryDto {

    private Long id;
    private String username;
    private String firstName;
    private String lastName;
    private String email;
    private String role;

    public String getFullName() {
        return firstName + " " + lastName;
    }

}

