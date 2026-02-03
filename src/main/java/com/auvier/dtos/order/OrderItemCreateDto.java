package com.auvier.dtos.order;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for creating an Order Item (nested in OrderCreateDto).
 *
 * Why this structure:
 * - Only variantId and quantity from client
 * - Price NOT included - fetched from database to prevent manipulation
 * - orderId set by service when processing the order
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemCreateDto {

    @NotNull(message = "Product variant is required")
    private Long productVariantId;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;
}
