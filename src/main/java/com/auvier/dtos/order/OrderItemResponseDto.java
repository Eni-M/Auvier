package com.auvier.dtos.order;

import com.auvier.enums.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for returning Order Item data.
 *
 * Why this structure:
 * - Includes denormalized product info (product name might change, but order history shouldn't)
 * - unitPrice is the snapshot price at time of purchase
 * - subtotal computed for display
 * - Variant details included for complete order display
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemResponseDto {

    private Long id;

    // Product/Variant info denormalized for display
    // (even if product deleted, order history shows what was bought)
    private Long productVariantId;
    private String productName;
    private String variantName;
    private String color;
    private Size size;
    private String sku;
    private String imageUrl;

    private Integer quantity;
    private BigDecimal unitPrice;  // Price at time of purchase
    private BigDecimal subtotal;   // unitPrice * quantity
}

