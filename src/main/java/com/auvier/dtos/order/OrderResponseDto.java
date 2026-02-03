package com.auvier.dtos.order;

import com.auvier.dtos.user.UserSummaryDto;
import com.auvier.enums.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for returning full Order data.
 *
 * Why this structure:
 * - UserSummaryDto instead of full user (privacy + payload size)
 * - Items included with product details (needed for order review)
 * - Payment status/transactionId for tracking
 * - Both timestamps for order timeline display
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponseDto {

    private Long id;

    // Who placed the order - summary only
    private UserSummaryDto user;

    // Full item details for order display
    private List<OrderItemResponseDto> items;

    private BigDecimal totalAmount;
    private OrderStatus status;
    private String shippingAddress;

    // Payment info
    private String paymentMethod;
    private String paymentStatus;
    private String transactionId;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Computed field - number of items for quick display
    private Integer itemCount;
}

