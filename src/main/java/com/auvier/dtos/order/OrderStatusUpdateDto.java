package com.auvier.dtos.order;

import com.auvier.enums.OrderStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for updating Order status (admin operation).
 *
 * Why this structure:
 * - Only status can be updated post-creation (business rule)
 * - Items cannot be modified after order is placed
 * - Shipping address changes might be separate workflow
 * - Payment info updated via payment processing callbacks
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatusUpdateDto {

    @NotNull(message = "Status is required")
    private OrderStatus status;
}

