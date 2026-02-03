package com.auvier.infrastructure.services.impl;

import com.auvier.dtos.order.*;
import com.auvier.entities.OrderEntity;
import com.auvier.entities.OrderItemEntity;
import com.auvier.entities.UserEntity;
import com.auvier.entities.catalog.ProductVariantEntity;
import com.auvier.enums.OrderStatus;
import com.auvier.exception.ResourceNotFoundException;
import com.auvier.infrastructure.services.InventoryService;
import com.auvier.infrastructure.services.OrderService;
import com.auvier.mappers.OrderMapper;
import com.auvier.repositories.OrderItemRepository;
import com.auvier.repositories.OrderRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final InventoryService inventoryService;
    private final OrderMapper orderMapper;

    // ==================== ORDER CRUD ====================

    @Override
    public OrderResponseDto createOrder(UserEntity user, OrderCreateDto dto) {
        log.info("Creating order for user: {}", user.getUsername());

        // Create order entity
        OrderEntity order = orderMapper.toEntity(dto);
        order.setUser(user);
        order.setStatus(OrderStatus.PENDING);
        order.setPaymentStatus("PENDING");
        order.setTotalAmount(BigDecimal.ZERO);

        // Save order first to get ID
        order = orderRepository.save(order);

        // Add items and calculate total
        BigDecimal total = BigDecimal.ZERO;
        for (OrderItemCreateDto itemDto : dto.getItems()) {
            // Validate and reserve stock
            inventoryService.validateStock(itemDto.getProductVariantId(), itemDto.getQuantity());
            inventoryService.reserveStock(itemDto.getProductVariantId(), itemDto.getQuantity());

            // Get variant for price
            ProductVariantEntity variant = inventoryService.getVariant(itemDto.getProductVariantId());

            // Create order item
            OrderItemEntity item = new OrderItemEntity();
            item.setOrder(order);
            item.setProductVariant(variant);
            item.setQuantity(itemDto.getQuantity());
            item.setUnitPrice(variant.getPrice()); // Snapshot current price

            order.addOrderItem(item);
            total = total.add(item.getSubtotal());
        }

        order.setTotalAmount(total);
        order = orderRepository.save(order);

        log.info("Order created successfully. ID: {}, Total: {}", order.getId(), total);
        return orderMapper.toResponseDto(order);
    }

    @Override
    public OrderResponseDto getOrder(Long orderId) {
        OrderEntity order = findOrderById(orderId);
        return orderMapper.toResponseDto(order);
    }

    @Override
    public OrderResponseDto getOrderForUser(Long orderId, UserEntity user) {
        OrderEntity order = findOrderById(orderId);
        if (!order.getUser().getId().equals(user.getId())) {
            throw new IllegalStateException("Order does not belong to this user");
        }
        return orderMapper.toResponseDto(order);
    }

    @Override
    public List<OrderSummaryDto> getAllOrders() {
        return orderMapper.toSummaryDtoList(orderRepository.findAll());
    }

    @Override
    public List<OrderSummaryDto> getOrdersForUser(UserEntity user) {
        // We need to add a query method to repository
        return orderRepository.findAll().stream()
                .filter(o -> o.getUser().getId().equals(user.getId()))
                .map(orderMapper::toSummaryDto)
                .toList();
    }

    @Override
    public void deleteOrder(Long orderId) {
        OrderEntity order = findOrderById(orderId);
        validateOrderModifiable(order);

        // Release all reserved stock
        for (OrderItemEntity item : order.getOrderItems()) {
            inventoryService.releaseStock(item.getProductVariant().getId(), item.getQuantity());
        }

        orderRepository.delete(order);
        log.info("Order {} deleted", orderId);
    }

    // ==================== ORDER ITEM MANAGEMENT ====================

    @Override
    public OrderResponseDto addItem(Long orderId, OrderItemCreateDto dto) {
        OrderEntity order = findOrderById(orderId);
        validateOrderModifiable(order);

        // Check if item already exists in order
        var existingItem = orderItemRepository.findByOrderIdAndProductVariantId(
                orderId, dto.getProductVariantId()
        );

        if (existingItem.isPresent()) {
            // Update quantity instead of adding duplicate
            OrderItemEntity item = existingItem.get();
            int newQuantity = item.getQuantity() + dto.getQuantity();

            // Validate additional stock needed
            inventoryService.validateStock(dto.getProductVariantId(), dto.getQuantity());
            inventoryService.reserveStock(dto.getProductVariantId(), dto.getQuantity());

            item.setQuantity(newQuantity);
            orderItemRepository.save(item);

            log.info("Updated item quantity in order {}. Variant: {}, New Qty: {}",
                    orderId, dto.getProductVariantId(), newQuantity);
        } else {
            // Add new item
            inventoryService.validateStock(dto.getProductVariantId(), dto.getQuantity());
            inventoryService.reserveStock(dto.getProductVariantId(), dto.getQuantity());

            ProductVariantEntity variant = inventoryService.getVariant(dto.getProductVariantId());

            OrderItemEntity item = new OrderItemEntity();
            item.setOrder(order);
            item.setProductVariant(variant);
            item.setQuantity(dto.getQuantity());
            item.setUnitPrice(variant.getPrice());

            order.addOrderItem(item);
            orderItemRepository.save(item);

            log.info("Added new item to order {}. Variant: {}, Qty: {}",
                    orderId, dto.getProductVariantId(), dto.getQuantity());
        }

        return recalculateTotal(orderId);
    }

    @Override
    public OrderResponseDto updateItemQuantity(Long orderId, Long itemId, int newQuantity) {
        OrderEntity order = findOrderById(orderId);
        validateOrderModifiable(order);

        OrderItemEntity item = orderItemRepository.findByIdAndOrderId(itemId, orderId)
                .orElseThrow(() -> new ResourceNotFoundException("OrderItem", itemId));

        if (newQuantity <= 0) {
            return removeItem(orderId, itemId);
        }

        int oldQuantity = item.getQuantity();
        Long variantId = item.getProductVariant().getId();

        // Adjust stock based on quantity change
        inventoryService.adjustStock(variantId, oldQuantity, newQuantity);

        item.setQuantity(newQuantity);
        orderItemRepository.save(item);

        log.info("Updated item {} quantity in order {}. Old: {}, New: {}",
                itemId, orderId, oldQuantity, newQuantity);

        return recalculateTotal(orderId);
    }

    @Override
    public OrderResponseDto removeItem(Long orderId, Long itemId) {
        OrderEntity order = findOrderById(orderId);
        validateOrderModifiable(order);

        OrderItemEntity item = orderItemRepository.findByIdAndOrderId(itemId, orderId)
                .orElseThrow(() -> new ResourceNotFoundException("OrderItem", itemId));

        // Release reserved stock
        inventoryService.releaseStock(item.getProductVariant().getId(), item.getQuantity());

        order.getOrderItems().remove(item);
        orderItemRepository.delete(item);

        log.info("Removed item {} from order {}. Released {} units of variant {}",
                itemId, orderId, item.getQuantity(), item.getProductVariant().getId());

        return recalculateTotal(orderId);
    }

    @Override
    public OrderResponseDto clearItems(Long orderId) {
        OrderEntity order = findOrderById(orderId);
        validateOrderModifiable(order);

        // Release all reserved stock
        for (OrderItemEntity item : order.getOrderItems()) {
            inventoryService.releaseStock(item.getProductVariant().getId(), item.getQuantity());
        }

        order.getOrderItems().clear();
        order.setTotalAmount(BigDecimal.ZERO);
        orderRepository.save(order);

        log.info("Cleared all items from order {}", orderId);
        return orderMapper.toResponseDto(order);
    }

    // ==================== STOCK VALIDATION ====================

    @Override
    public boolean checkStock(Long variantId, int quantity) {
        return inventoryService.hasStock(variantId, quantity);
    }

    @Override
    public boolean isVariantAvailable(Long variantId) {
        try {
            ProductVariantEntity variant = inventoryService.getVariant(variantId);
            return variant.isActive() && variant.getStock() > 0;
        } catch (ResourceNotFoundException e) {
            return false;
        }
    }

    // ==================== ORDER STATUS WORKFLOW ====================

    @Override
    public OrderResponseDto updateStatus(Long orderId, OrderStatusUpdateDto dto) {
        OrderEntity order = findOrderById(orderId);
        OrderStatus newStatus = dto.getStatus();

        validateStatusTransition(order.getStatus(), newStatus);

        order.setStatus(newStatus);
        orderRepository.save(order);

        log.info("Order {} status updated to {}", orderId, newStatus);
        return orderMapper.toResponseDto(order);
    }

    @Override
    public OrderResponseDto confirmOrder(Long orderId) {
        OrderEntity order = findOrderById(orderId);

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalStateException("Only PENDING orders can be confirmed");
        }

        if (order.getOrderItems().isEmpty()) {
            throw new IllegalStateException("Cannot confirm order with no items");
        }

        // Final stock validation before confirmation
        for (OrderItemEntity item : order.getOrderItems()) {
            // Stock already reserved, just validate variant is still active
            if (!item.getProductVariant().isActive()) {
                throw new IllegalStateException(
                        "Product variant " + item.getProductVariant().getSku() + " is no longer available"
                );
            }
        }

        order.setStatus(OrderStatus.CREATED);
        orderRepository.save(order);

        log.info("Order {} confirmed", orderId);
        return orderMapper.toResponseDto(order);
    }

    @Override
    public OrderResponseDto cancelOrder(Long orderId, String reason) {
        OrderEntity order = findOrderById(orderId);

        if (order.getStatus() == OrderStatus.DELIVERED) {
            throw new IllegalStateException("Cannot cancel delivered order");
        }

        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new IllegalStateException("Order already cancelled");
        }

        // Release all reserved stock
        for (OrderItemEntity item : order.getOrderItems()) {
            inventoryService.releaseStock(item.getProductVariant().getId(), item.getQuantity());
        }

        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);

        log.info("Order {} cancelled. Reason: {}", orderId, reason);
        return orderMapper.toResponseDto(order);
    }

    @Override
    public OrderResponseDto markAsPaid(Long orderId, String transactionId) {
        OrderEntity order = findOrderById(orderId);

        if (order.getStatus() != OrderStatus.CREATED && order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalStateException("Order must be CREATED or PENDING to mark as PAID");
        }

        order.setStatus(OrderStatus.PAID);
        order.setPaymentStatus("PAID");
        order.setTransactionId(transactionId);
        orderRepository.save(order);

        log.info("Order {} marked as paid. Transaction: {}", orderId, transactionId);
        return orderMapper.toResponseDto(order);
    }

    @Override
    public OrderResponseDto markAsShipped(Long orderId) {
        OrderEntity order = findOrderById(orderId);

        if (order.getStatus() != OrderStatus.PAID) {
            throw new IllegalStateException("Order must be PAID before shipping");
        }

        order.setStatus(OrderStatus.SHIPPED);
        orderRepository.save(order);

        log.info("Order {} marked as shipped", orderId);
        return orderMapper.toResponseDto(order);
    }

    @Override
    public OrderResponseDto markAsDelivered(Long orderId) {
        OrderEntity order = findOrderById(orderId);

        if (order.getStatus() != OrderStatus.SHIPPED) {
            throw new IllegalStateException("Order must be SHIPPED before delivery");
        }

        order.setStatus(OrderStatus.DELIVERED);
        orderRepository.save(order);

        log.info("Order {} marked as delivered", orderId);
        return orderMapper.toResponseDto(order);
    }

    // ==================== ORDER CALCULATIONS ====================

    @Override
    public OrderResponseDto recalculateTotal(Long orderId) {
        OrderEntity order = findOrderById(orderId);

        BigDecimal total = order.getOrderItems().stream()
                .map(OrderItemEntity::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        order.setTotalAmount(total);
        order = orderRepository.save(order);

        return orderMapper.toResponseDto(order);
    }

    // ==================== PRIVATE HELPER METHODS ====================

    private OrderEntity findOrderById(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));
    }

    private void validateOrderModifiable(OrderEntity order) {
        if (order.getStatus() == OrderStatus.SHIPPED ||
                order.getStatus() == OrderStatus.DELIVERED ||
                order.getStatus() == OrderStatus.CANCELLED) {
            throw new IllegalStateException(
                    "Cannot modify order in status: " + order.getStatus()
            );
        }
    }

    private void validateStatusTransition(OrderStatus current, OrderStatus target) {
        // Define valid transitions
        boolean valid = switch (current) {
            case PENDING -> target == OrderStatus.CREATED || target == OrderStatus.CANCELLED;
            case CREATED -> target == OrderStatus.PAID || target == OrderStatus.CANCELLED;
            case PAID -> target == OrderStatus.SHIPPED || target == OrderStatus.CANCELLED;
            case SHIPPED -> target == OrderStatus.DELIVERED;
            case DELIVERED, CANCELLED -> false; // Terminal states
        };

        if (!valid) {
            throw new IllegalStateException(
                    "Invalid status transition from " + current + " to " + target
            );
        }
    }
}
