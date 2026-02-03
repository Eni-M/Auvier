package com.auvier.infrastructure.services;

import com.auvier.dtos.order.*;
import com.auvier.entities.UserEntity;
import com.auvier.enums.OrderStatus;

import java.util.List;

/**
 * Service for managing orders throughout their lifecycle.
 * Handles order creation, item management, status transitions, and stock validation.
 */
public interface OrderService {

    // ==================== ORDER CRUD ====================

    /**
     * Create a new order for a user.
     * @param user the authenticated user
     * @param dto order creation data
     * @return the created order
     */
    OrderResponseDto createOrder(UserEntity user, OrderCreateDto dto);

    /**
     * Get order by ID with full details.
     * @param orderId the order ID
     * @return full order response
     */
    OrderResponseDto getOrder(Long orderId);

    /**
     * Get order by ID, ensuring it belongs to the user.
     * @param orderId the order ID
     * @param user the user who should own the order
     * @return full order response
     */
    OrderResponseDto getOrderForUser(Long orderId, UserEntity user);

    /**
     * Get all orders (admin).
     * @return list of order summaries
     */
    List<OrderSummaryDto> getAllOrders();

    /**
     * Get orders for a specific user.
     * @param user the user
     * @return list of order summaries
     */
    List<OrderSummaryDto> getOrdersForUser(UserEntity user);

    /**
     * Delete/cancel an order.
     * @param orderId the order ID
     */
    void deleteOrder(Long orderId);

    // ==================== ORDER ITEM MANAGEMENT ====================

    /**
     * Add an item to an existing order.
     * Validates stock availability before adding.
     * @param orderId the order ID
     * @param dto item to add
     * @return updated order
     */
    OrderResponseDto addItem(Long orderId, OrderItemCreateDto dto);

    /**
     * Update quantity of an existing order item.
     * Validates stock availability for quantity increase.
     * @param orderId the order ID
     * @param itemId the order item ID
     * @param newQuantity the new quantity
     * @return updated order
     */
    OrderResponseDto updateItemQuantity(Long orderId, Long itemId, int newQuantity);

    /**
     * Remove an item from an order.
     * Releases reserved stock back to inventory.
     * @param orderId the order ID
     * @param itemId the order item ID
     * @return updated order
     */
    OrderResponseDto removeItem(Long orderId, Long itemId);

    /**
     * Clear all items from an order.
     * Releases all reserved stock.
     * @param orderId the order ID
     * @return updated order (empty)
     */
    OrderResponseDto clearItems(Long orderId);

    // ==================== STOCK VALIDATION ====================

    /**
     * Check if a product variant has sufficient stock.
     * @param variantId the product variant ID
     * @param quantity required quantity
     * @return true if stock is available
     */
    boolean checkStock(Long variantId, int quantity);

    /**
     * Check if a variant exists and is purchasable.
     * @param variantId the product variant ID
     * @return true if variant exists and is active
     */
    boolean isVariantAvailable(Long variantId);

    // ==================== ORDER STATUS WORKFLOW ====================

    /**
     * Update order status (admin).
     * @param orderId the order ID
     * @param dto status update data
     * @return updated order
     */
    OrderResponseDto updateStatus(Long orderId, OrderStatusUpdateDto dto);

    /**
     * Confirm an order (move from PENDING to CREATED).
     * Final stock validation before confirmation.
     * @param orderId the order ID
     * @return confirmed order
     */
    OrderResponseDto confirmOrder(Long orderId);

    /**
     * Cancel an order.
     * Releases all reserved stock back to inventory.
     * @param orderId the order ID
     * @param reason cancellation reason
     * @return cancelled order
     */
    OrderResponseDto cancelOrder(Long orderId, String reason);

    /**
     * Mark order as paid.
     * @param orderId the order ID
     * @param transactionId payment transaction ID
     * @return updated order
     */
    OrderResponseDto markAsPaid(Long orderId, String transactionId);

    /**
     * Mark order as shipped.
     * @param orderId the order ID
     * @return updated order
     */
    OrderResponseDto markAsShipped(Long orderId);

    /**
     * Mark order as delivered.
     * @param orderId the order ID
     * @return updated order
     */
    OrderResponseDto markAsDelivered(Long orderId);

    // ==================== ORDER CALCULATIONS ====================

    /**
     * Recalculate and update order total based on items.
     * @param orderId the order ID
     * @return recalculated total
     */
    OrderResponseDto recalculateTotal(Long orderId);
}
