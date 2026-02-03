package com.auvier.dtos.order;

import com.auvier.enums.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Lightweight DTO for order listings (user's order history, admin order list).
 *
 * Why needed:
 * - Order list doesn't need full item details
 * - Reduces payload for paginated order lists
 * - Quick overview information only
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderSummaryDto {

    private Long id;
    private BigDecimal totalAmount;
    private OrderStatus status;
    private Integer itemCount;
    private LocalDateTime createdAt;

    // For admin views - whose order is this
    private String customerUsername;
}

