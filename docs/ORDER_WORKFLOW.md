# Order Workflow Implementation

This document details the complete order workflow system implemented for the Auvier e-commerce platform.

---

## Table of Contents

1. [Overview](#overview)
2. [Order Lifecycle](#order-lifecycle)
3. [File Structure](#file-structure)
4. [Services](#services)
5. [Repositories](#repositories)
6. [Mappers](#mappers)
7. [Controllers](#controllers)
8. [Templates](#templates)
9. [API Endpoints](#api-endpoints)
10. [Key Code Snippets](#key-code-snippets)

---

## Overview

The order workflow handles the complete lifecycle of customer orders:
- Creating orders from cart items
- Managing order items (add/update/remove)
- Stock validation and reservation
- Order status transitions
- Admin order management

---

## Order Lifecycle

```
┌─────────────┐
│   PENDING   │ ← Order created, items added
└──────┬──────┘
       │ confirmOrder()
       ▼
┌─────────────┐
│   CREATED   │ ← Order confirmed, ready for payment
└──────┬──────┘
       │ markAsPaid()
       ▼
┌─────────────┐
│    PAID     │ ← Payment received
└──────┬──────┘
       │ markAsShipped()
       ▼
┌─────────────┐
│   SHIPPED   │ ← Order dispatched
└──────┬──────┘
       │ markAsDelivered()
       ▼
┌─────────────┐
│  DELIVERED  │ ← Final state (complete)
└─────────────┘

       │
       │ cancelOrder() (from PENDING, CREATED, PAID, or SHIPPED)
       ▼
┌─────────────┐
│  CANCELLED  │ ← Final state (stock released)
└─────────────┘
```

---

## File Structure

```
src/main/java/com/auvier/
├── infrastructure/services/
│   ├── InventoryService.java          # Stock management interface
│   ├── OrderService.java              # Order workflow interface
│   └── impl/
│       ├── InventoryServiceImpl.java  # Stock implementation
│       └── OrderServiceImpl.java      # Order implementation
├── repositories/
│   ├── OrderRepository.java           # Order data access
│   └── OrderItemRepository.java       # Order item data access
├── mappers/
│   └── OrderMapper.java               # Entity ↔ DTO mapping
└── controllers/
    ├── admin/
    │   └── OrderController.java       # Admin Thymeleaf controller
    └── OrderApiController.java        # REST API controller

src/main/resources/templates/admin/orders/
├── list.html                          # Order listing page
├── view.html                          # Order details page
└── status.html                        # Status update form
```

---

## Services

### InventoryService

Manages product variant stock levels.

```java
public interface InventoryService {

    /**
     * Check if a variant has sufficient stock.
     */
    boolean hasStock(Long variantId, int quantity);

    /**
     * Validate stock availability - throws exception if insufficient.
     */
    void validateStock(Long variantId, int quantity);

    /**
     * Get current stock level for a variant.
     */
    int getStock(Long variantId);

    /**
     * Reserve stock (decrement) when adding to order.
     */
    void reserveStock(Long variantId, int quantity);

    /**
     * Release reserved stock (increment) when removing from order or cancelling.
     */
    void releaseStock(Long variantId, int quantity);

    /**
     * Adjust stock when updating order item quantity.
     */
    void adjustStock(Long variantId, int oldQuantity, int newQuantity);

    /**
     * Get the variant entity (for price lookup, etc.).
     */
    ProductVariantEntity getVariant(Long variantId);
}
```

#### InventoryServiceImpl - Key Methods

```java
@Service
@Transactional
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private final ProductVariantRepository variantRepository;

    @Override
    public void validateStock(Long variantId, int quantity) {
        ProductVariantEntity variant = getVariant(variantId);

        if (!variant.isActive()) {
            throw new IllegalStateException(
                "Product variant '" + variant.getSku() + "' is not available for purchase"
            );
        }

        if (variant.getStock() < quantity) {
            throw new IllegalStateException(
                "Insufficient stock for '" + variant.getSku() + "'. " +
                "Available: " + variant.getStock() + ", Requested: " + quantity
            );
        }
    }

    @Override
    public void reserveStock(Long variantId, int quantity) {
        validateStock(variantId, quantity);
        ProductVariantEntity variant = getVariant(variantId);
        variant.setStock(variant.getStock() - quantity);
        variantRepository.save(variant);
    }

    @Override
    public void releaseStock(Long variantId, int quantity) {
        ProductVariantEntity variant = getVariant(variantId);
        variant.setStock(variant.getStock() + quantity);
        variantRepository.save(variant);
    }

    @Override
    public void adjustStock(Long variantId, int oldQuantity, int newQuantity) {
        int difference = newQuantity - oldQuantity;

        if (difference > 0) {
            // Need more stock - validate and reserve additional
            validateStock(variantId, difference);
            reserveStock(variantId, difference);
        } else if (difference < 0) {
            // Releasing stock back
            releaseStock(variantId, Math.abs(difference));
        }
        // If difference == 0, no change needed
    }
}
```

---

### OrderService

Complete order workflow interface.

```java
public interface OrderService {

    // ==================== ORDER CRUD ====================
    OrderResponseDto createOrder(UserEntity user, OrderCreateDto dto);
    OrderResponseDto getOrder(Long orderId);
    OrderResponseDto getOrderForUser(Long orderId, UserEntity user);
    List<OrderSummaryDto> getAllOrders();
    List<OrderSummaryDto> getOrdersForUser(UserEntity user);
    void deleteOrder(Long orderId);

    // ==================== ORDER ITEM MANAGEMENT ====================
    OrderResponseDto addItem(Long orderId, OrderItemCreateDto dto);
    OrderResponseDto updateItemQuantity(Long orderId, Long itemId, int newQuantity);
    OrderResponseDto removeItem(Long orderId, Long itemId);
    OrderResponseDto clearItems(Long orderId);

    // ==================== STOCK VALIDATION ====================
    boolean checkStock(Long variantId, int quantity);
    boolean isVariantAvailable(Long variantId);

    // ==================== ORDER STATUS WORKFLOW ====================
    OrderResponseDto updateStatus(Long orderId, OrderStatusUpdateDto dto);
    OrderResponseDto confirmOrder(Long orderId);
    OrderResponseDto cancelOrder(Long orderId, String reason);
    OrderResponseDto markAsPaid(Long orderId, String transactionId);
    OrderResponseDto markAsShipped(Long orderId);
    OrderResponseDto markAsDelivered(Long orderId);

    // ==================== ORDER CALCULATIONS ====================
    OrderResponseDto recalculateTotal(Long orderId);
}
```

#### OrderServiceImpl - Key Methods

**Creating an Order:**

```java
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
```

**Adding an Item:**

```java
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
    }

    return recalculateTotal(orderId);
}
```

**Cancelling an Order:**

```java
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
```

**Status Transition Validation:**

```java
private void validateStatusTransition(OrderStatus current, OrderStatus target) {
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
```

---

## Repositories

### OrderRepository

```java
@Repository
public interface OrderRepository extends JpaRepository<OrderEntity, Long> {

    List<OrderEntity> findByUserId(Long userId);

    List<OrderEntity> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<OrderEntity> findByStatus(OrderStatus status);

    @Query("SELECT o FROM OrderEntity o WHERE o.user.username = :username ORDER BY o.createdAt DESC")
    List<OrderEntity> findByUsername(@Param("username") String username);

    @Query("SELECT COUNT(o) FROM OrderEntity o WHERE o.user.id = :userId")
    long countByUserId(@Param("userId") Long userId);

    @Query("SELECT COUNT(o) FROM OrderEntity o WHERE o.status = :status")
    long countByStatus(@Param("status") OrderStatus status);
}
```

### OrderItemRepository

```java
@Repository
public interface OrderItemRepository extends JpaRepository<OrderItemEntity, Long> {

    List<OrderItemEntity> findByOrderId(Long orderId);

    Optional<OrderItemEntity> findByOrderIdAndProductVariantId(Long orderId, Long productVariantId);

    Optional<OrderItemEntity> findByIdAndOrderId(Long id, Long orderId);

    void deleteByOrderId(Long orderId);

    @Query("SELECT COUNT(oi) FROM OrderItemEntity oi WHERE oi.order.id = :orderId")
    int countByOrderId(@Param("orderId") Long orderId);

    boolean existsByOrderIdAndProductVariantId(Long orderId, Long productVariantId);
}
```

---

## Mappers

### OrderMapper

```java
@Mapper(componentModel = "spring")
public interface OrderMapper {

    // ==================== ORDER MAPPINGS ====================

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "orderItems", ignore = true)
    @Mapping(target = "totalAmount", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "paymentStatus", ignore = true)
    @Mapping(target = "transactionId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    OrderEntity toEntity(OrderCreateDto dto);

    @Mapping(target = "user", source = "user")
    @Mapping(target = "items", source = "orderItems")
    @Mapping(target = "itemCount", expression = "java(entity.getOrderItems() != null ? entity.getOrderItems().size() : 0)")
    OrderResponseDto toResponseDto(OrderEntity entity);

    @Mapping(target = "itemCount", expression = "java(entity.getOrderItems() != null ? entity.getOrderItems().size() : 0)")
    @Mapping(target = "customerUsername", source = "user.username")
    OrderSummaryDto toSummaryDto(OrderEntity entity);

    // ==================== ORDER ITEM MAPPINGS ====================

    @Mapping(target = "productName", source = "productVariant.product.name")
    @Mapping(target = "variantName", expression = "java(buildVariantName(item.getProductVariant()))")
    @Mapping(target = "color", source = "productVariant.color")
    @Mapping(target = "size", source = "productVariant.size")
    @Mapping(target = "sku", source = "productVariant.sku")
    @Mapping(target = "imageUrl", ignore = true)
    @Mapping(target = "subtotal", expression = "java(calculateSubtotal(item))")
    OrderItemResponseDto toItemResponseDto(OrderItemEntity item);

    default String buildVariantName(ProductVariantEntity variant) {
        StringBuilder sb = new StringBuilder();
        if (variant.getColor() != null) sb.append(variant.getColor());
        if (variant.getSize() != null) {
            if (sb.length() > 0) sb.append(" / ");
            sb.append(variant.getSize().name());
        }
        return sb.length() > 0 ? sb.toString() : variant.getSku();
    }

    default BigDecimal calculateSubtotal(OrderItemEntity item) {
        return item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
    }
}
```

---

## Controllers

### Admin OrderController (Thymeleaf)

```java
@Controller
@RequestMapping("/admin/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("orders", orderService.getAllOrders());
        return "admin/orders/list";
    }

    @GetMapping("/{id}")
    public String view(@PathVariable Long id, Model model) {
        model.addAttribute("order", orderService.getOrder(id));
        return "admin/orders/view";
    }

    @PostMapping("/{id}/confirm")
    public String confirmOrder(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            orderService.confirmOrder(id);
            redirectAttributes.addFlashAttribute("success", "Order confirmed successfully");
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/orders/" + id;
    }

    @PostMapping("/{id}/cancel")
    public String cancelOrder(@PathVariable Long id,
                              @RequestParam(defaultValue = "Cancelled by admin") String reason,
                              RedirectAttributes redirectAttributes) {
        try {
            orderService.cancelOrder(id, reason);
            redirectAttributes.addFlashAttribute("success", "Order cancelled successfully");
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/orders/" + id;
    }

    // ... more endpoints for mark-paid, mark-shipped, mark-delivered
}
```

### REST API Controller

```java
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderApiController {

    private final OrderService orderService;
    private final UserService userService;

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
}
```

---

## API Endpoints

### Customer-Facing (REST API)

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/orders` | Create new order |
| `GET` | `/api/orders` | Get current user's orders |
| `GET` | `/api/orders/{id}` | Get order details |
| `POST` | `/api/orders/{id}/items` | Add item to order |
| `PATCH` | `/api/orders/{id}/items/{itemId}` | Update item quantity |
| `DELETE` | `/api/orders/{id}/items/{itemId}` | Remove item |
| `DELETE` | `/api/orders/{id}/items` | Clear all items |
| `GET` | `/api/orders/check-stock?variantId=X&quantity=Y` | Check stock availability |
| `POST` | `/api/orders/{id}/confirm` | Confirm order |
| `POST` | `/api/orders/{id}/cancel` | Cancel order |

### Admin (Thymeleaf)

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/admin/orders` | List all orders |
| `GET` | `/admin/orders/{id}` | View order details |
| `GET` | `/admin/orders/{id}/status` | Status update form |
| `POST` | `/admin/orders/{id}/status` | Update status |
| `POST` | `/admin/orders/{id}/confirm` | Confirm order |
| `POST` | `/admin/orders/{id}/cancel` | Cancel order |
| `POST` | `/admin/orders/{id}/mark-paid` | Mark as paid |
| `POST` | `/admin/orders/{id}/mark-shipped` | Mark as shipped |
| `POST` | `/admin/orders/{id}/mark-delivered` | Mark as delivered |
| `POST` | `/admin/orders/{id}/delete` | Delete order |

---

## Templates

### Navigation Update

Added Orders link to admin navigation in `_layout.html`:

```html
<nav class="v-topnav">
    <a class="v-topnav__link" th:href="@{/admin}">Dashboard</a>
    <a class="v-topnav__link" th:href="@{/admin/orders}">Orders</a>
    <a class="v-topnav__link" th:href="@{/admin/categories}">Categories</a>
    <a class="v-topnav__link" th:href="@{/admin/products}">Products</a>
</nav>
```

### Order List Table

```html
<tr th:each="o : ${orders}">
    <td class="v-mono" th:text="${o.id}">1</td>
    <td>
        <div class="v-strong" th:text="${o.customerUsername}">customer</div>
    </td>
    <td th:text="${o.itemCount + ' item(s)'}">1 item(s)</td>
    <td class="v-strong" th:text="${'$' + #numbers.formatDecimal(o.totalAmount, 1, 2)}">$0.00</td>
    <td>
        <span class="v-badge"
              th:classappend="${o.status.name() == 'DELIVERED'} ? ' v-badge--ok' :
                              (${o.status.name() == 'CANCELLED'} ? ' v-badge--danger' :
                              (${o.status.name() == 'SHIPPED'} ? ' v-badge--info' :
                              (${o.status.name() == 'PAID'} ? ' v-badge--success' : ' v-badge--muted')))"
              th:text="${o.status}">
            PENDING
        </span>
    </td>
    <td th:text="${#temporals.format(o.createdAt, 'MMM dd, yyyy')}">Jan 01, 2026</td>
    <td class="v-td-right">
        <div class="v-btngroup">
            <a class="v-btn v-btn--ghost v-btn--sm"
               th:href="@{/admin/orders/{id}(id=${o.id})}">View</a>
            <a th:href="@{/admin/orders/{id}/status(id=${o.id})}"
               class="v-btn v-btn--ghost v-btn--sm">Status</a>
        </div>
    </td>
</tr>
```

### Quick Status Actions (Order View)

```html
<div class="v-stack-sm">
    <form th:if="${order.status.name() == 'PENDING'}"
          th:action="@{/admin/orders/{id}/confirm(id=${order.id})}" method="post">
        <button type="submit" class="v-btn v-btn--primary w-100">Confirm Order</button>
    </form>

    <form th:if="${order.status.name() == 'CREATED' or order.status.name() == 'PENDING'}"
          th:action="@{/admin/orders/{id}/mark-paid(id=${order.id})}" method="post">
        <button type="submit" class="v-btn v-btn--success w-100">Mark as Paid</button>
    </form>

    <form th:if="${order.status.name() == 'PAID'}"
          th:action="@{/admin/orders/{id}/mark-shipped(id=${order.id})}" method="post">
        <button type="submit" class="v-btn v-btn--info w-100">Mark as Shipped</button>
    </form>

    <form th:if="${order.status.name() == 'SHIPPED'}"
          th:action="@{/admin/orders/{id}/mark-delivered(id=${order.id})}" method="post">
        <button type="submit" class="v-btn v-btn--ok w-100">Mark as Delivered</button>
    </form>

    <form th:if="${order.status.name() != 'DELIVERED' and order.status.name() != 'CANCELLED'}"
          th:action="@{/admin/orders/{id}/cancel(id=${order.id})}" method="post">
        <button type="submit" class="v-btn v-btn--danger v-btn--ghost w-100"
                onclick="return confirm('Cancel this order?')">Cancel Order</button>
    </form>
</div>
```

---

## Key Features Summary

| Feature | Description |
|---------|-------------|
| **Stock Validation** | Checks `ProductVariantEntity.stock` before adding items |
| **Stock Reservation** | Decrements stock immediately when items added |
| **Stock Release** | Restores stock when items removed or order cancelled |
| **Price Snapshot** | Stores price at time of purchase in `OrderItemEntity.unitPrice` |
| **Status Workflow** | Enforces valid transitions (PENDING → CREATED → PAID → SHIPPED → DELIVERED) |
| **Admin UI** | Full Thymeleaf-based order management |
| **REST API** | Customer-facing endpoints for checkout flow |
| **Transactional** | All service methods are transactional for data consistency |

---

## Usage Examples

### Creating an Order (API)

```bash
POST /api/orders
Content-Type: application/json

{
  "items": [
    { "productVariantId": 1, "quantity": 2 },
    { "productVariantId": 5, "quantity": 1 }
  ],
  "shippingAddress": "123 Main St, City, Country",
  "paymentMethod": "CREDIT_CARD"
}
```

### Checking Stock (API)

```bash
GET /api/orders/check-stock?variantId=1&quantity=5

Response:
{
  "variantId": 1,
  "requestedQuantity": 5,
  "available": true,
  "variantActive": true
}
```

### Adding Item to Order (API)

```bash
POST /api/orders/42/items
Content-Type: application/json

{
  "productVariantId": 3,
  "quantity": 2
}
```

---

*Document created: January 28, 2026*
