package com.auvier.controllers;

import com.auvier.dtos.order.*;
import com.auvier.entities.UserEntity;
import com.auvier.infrastructure.services.OrderService;
import com.auvier.infrastructure.services.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST API for customer order operations.
 * Handles order creation, item management, and checkout flow.
 */
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderApiController {

    private final OrderService orderService;
    private final UserService userService;

    // ==================== ORDER CRUD ====================

    /**
     * Create a new order from cart items.
     */
    @PostMapping
    public ResponseEntity<OrderResponseDto> createOrder(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody OrderCreateDto dto) {

        UserEntity user = userService.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new IllegalStateException("User not found"));

        OrderResponseDto order = orderService.createOrder(user, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(order);
    }

    /**
     * Get current user's orders.
     */
    @GetMapping
    public ResponseEntity<List<OrderSummaryDto>> getMyOrders(
            @AuthenticationPrincipal UserDetails userDetails) {

        UserEntity user = userService.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new IllegalStateException("User not found"));

        return ResponseEntity.ok(orderService.getOrdersForUser(user));
    }

    /**
     * Get specific order details.
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponseDto> getOrder(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long orderId) {

        UserEntity user = userService.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new IllegalStateException("User not found"));

        return ResponseEntity.ok(orderService.getOrderForUser(orderId, user));
    }

    // ==================== ITEM MANAGEMENT ====================

    /**
     * Add item to existing order.
     */
    @PostMapping("/{orderId}/items")
    public ResponseEntity<OrderResponseDto> addItem(
            @PathVariable Long orderId,
            @Valid @RequestBody OrderItemCreateDto dto) {

        return ResponseEntity.ok(orderService.addItem(orderId, dto));
    }

    /**
     * Update item quantity.
     */
    @PatchMapping("/{orderId}/items/{itemId}")
    public ResponseEntity<OrderResponseDto> updateItemQuantity(
            @PathVariable Long orderId,
            @PathVariable Long itemId,
            @RequestBody Map<String, Integer> payload) {

        int quantity = payload.getOrDefault("quantity", 1);
        return ResponseEntity.ok(orderService.updateItemQuantity(orderId, itemId, quantity));
    }

    /**
     * Remove item from order.
     */
    @DeleteMapping("/{orderId}/items/{itemId}")
    public ResponseEntity<OrderResponseDto> removeItem(
            @PathVariable Long orderId,
            @PathVariable Long itemId) {

        return ResponseEntity.ok(orderService.removeItem(orderId, itemId));
    }

    /**
     * Clear all items from order.
     */
    @DeleteMapping("/{orderId}/items")
    public ResponseEntity<OrderResponseDto> clearItems(@PathVariable Long orderId) {
        return ResponseEntity.ok(orderService.clearItems(orderId));
    }

    // ==================== STOCK VALIDATION ====================

    /**
     * Check if a variant has available stock.
     */
    @GetMapping("/check-stock")
    public ResponseEntity<Map<String, Object>> checkStock(
            @RequestParam Long variantId,
            @RequestParam(defaultValue = "1") int quantity) {

        boolean available = orderService.checkStock(variantId, quantity);
        boolean variantActive = orderService.isVariantAvailable(variantId);

        return ResponseEntity.ok(Map.of(
                "variantId", variantId,
                "requestedQuantity", quantity,
                "available", available,
                "variantActive", variantActive
        ));
    }

    // ==================== ORDER WORKFLOW ====================

    /**
     * Confirm order (finalize before payment).
     */
    @PostMapping("/{orderId}/confirm")
    public ResponseEntity<OrderResponseDto> confirmOrder(@PathVariable Long orderId) {
        return ResponseEntity.ok(orderService.confirmOrder(orderId));
    }

    /**
     * Cancel order.
     */
    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<OrderResponseDto> cancelOrder(
            @PathVariable Long orderId,
            @RequestBody(required = false) Map<String, String> payload) {

        String reason = payload != null ? payload.getOrDefault("reason", "Cancelled by customer") : "Cancelled by customer";
        return ResponseEntity.ok(orderService.cancelOrder(orderId, reason));
    }

    // ==================== EXCEPTION HANDLING ====================

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleIllegalState(IllegalStateException ex) {
        return ResponseEntity.badRequest().body(Map.of(
                "error", ex.getMessage()
        ));
    }
}
