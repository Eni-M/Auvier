package com.auvier.dtos.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for creating a new Order.
 *
 * Why this structure:
 * - No userId - determined from authenticated user (security!)
 * - No totalAmount - calculated server-side from items (prevents manipulation)
 * - No status - defaults to PENDING on creation
 * - Items as nested DTOs with @Valid for cascading validation
 * - Shipping/payment info required at checkout time
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreateDto {

    @NotEmpty(message = "Order must contain at least one item")
    @Valid
    private List<OrderItemCreateDto> items;

    @NotBlank(message = "Shipping address is required")
    private String shippingAddress;

    @NotBlank(message = "Payment method is required")
    private String paymentMethod;
}
