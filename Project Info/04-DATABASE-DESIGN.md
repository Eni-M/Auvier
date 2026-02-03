# 04 - Database Design

This document explains the database schema, entity relationships, and design decisions.

---

## Entity Relationship Diagram

```
┌─────────────────┐       ┌─────────────────┐
│     USERS       │       │    CATEGORIES   │
├─────────────────┤       ├─────────────────┤
│ id (PK)         │       │ id (PK)         │
│ username        │       │ name            │
│ email           │       │ slug            │
│ password        │       │ description     │
│ firstName       │       │ active          │
│ lastName        │       │ parent_id (FK)──┼──┐ (self-reference)
│ address         │       └────────┬────────┘  │
│ phone           │                │           │
│ role            │                │           │
│ createdAt       │                ▼           │
└────────┬────────┘       ┌─────────────────┐  │
         │                │    PRODUCTS     │  │
         │                ├─────────────────┤  │
         │                │ id (PK)         │  │
         │                │ name            │  │
         │                │ slug            │  │
         │                │ description     │  │
         │                │ active          │  │
         │                │ category_id(FK)─┼──┘
         │                │ sub_category_id │
         │                │ createdAt       │
         │                └────────┬────────┘
         │                         │
         │                         │ 1:N
         │                         ▼
         │                ┌─────────────────┐
         │                │PRODUCT_VARIANTS │
         │                ├─────────────────┤
         │                │ id (PK)         │
         │                │ product_id (FK) │
         │                │ sku             │
         │                │ price           │
         │                │ stock           │
         │                │ color           │
         │                │ size            │
         │                │ imageUrl        │
         │                │ active          │
         │                └────────┬────────┘
         │                         │
         │                         │
         ▼                         │
┌─────────────────┐                │
│     ORDERS      │                │
├─────────────────┤                │
│ id (PK)         │                │
│ user_id (FK)────┼────────────────┘
│ totalAmount     │
│ status          │
│ shippingAddress │
│ paymentMethod   │
│ paymentStatus   │
│ transactionId   │
│ createdAt       │
│ updatedAt       │
└────────┬────────┘
         │
         │ 1:N
         ▼
┌─────────────────┐
│   ORDER_ITEMS   │
├─────────────────┤
│ id (PK)         │
│ order_id (FK)   │
│ variant_id (FK) │
│ quantity        │
│ unitPrice       │ ← Price snapshot at time of order
└─────────────────┘

┌─────────────────────┐
│ ADMIN_ACTIVITY_LOGS │
├─────────────────────┤
│ id (PK)             │
│ action              │
│ entityType          │
│ entityId            │
│ entityName          │
│ description         │
│ changesDetail       │
│ previousValues      │
│ newValues           │
│ adminUsername       │
│ adminDisplayName    │
│ details             │
│ timestamp           │
│ ipAddress           │
│ userAgent           │
│ sessionId           │
└─────────────────────┘
```

---

## Table Details

### 1. USERS

Stores user accounts (customers and admins).

```java
@Entity
@Table(name = "users")
public class UserEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;  // BCrypt hashed

    private String firstName;
    private String lastName;
    private String address;
    private String phone;

    @Enumerated(EnumType.STRING)
    private Role role = Role.CUSTOMER;  // CUSTOMER or ADMIN

    private LocalDateTime createdAt;
}
```

**SQL Equivalent:**
```sql
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    address VARCHAR(500),
    phone VARCHAR(20),
    role VARCHAR(20) NOT NULL DEFAULT 'CUSTOMER',
    created_at TIMESTAMP
);
```

**Design Decisions:**
- `username` is unique for login
- `email` is unique for password recovery
- `password` stored as BCrypt hash (never plain text)
- `role` as ENUM string (easier to read in DB than integer)

---

### 2. CATEGORIES

Hierarchical category structure (parent-child).

```java
@Entity(name = "categories")
public class CategoryEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 60)
    private String name;

    @Column(nullable = false, unique = true, length = 80)
    private String slug;  // URL-friendly: "mens-shirts"

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private CategoryEntity parent;  // Self-reference for hierarchy

    @OneToMany(mappedBy = "parent")
    private List<CategoryEntity> children = new ArrayList<>();

    private boolean active = true;
    private String description;
}
```

**Why Self-Referencing?**

This allows unlimited category depth:
```
Clothing (parent_id = null)
├── Men's (parent_id = 1)
│   ├── Shirts (parent_id = 2)
│   └── Pants (parent_id = 2)
└── Women's (parent_id = 1)
    ├── Dresses (parent_id = 3)
    └── Tops (parent_id = 3)
```

**Why `slug`?**

URLs like `/shop/category/mens-shirts` are:
- Human-readable
- SEO-friendly
- Stable (don't change if name is edited slightly)

---

### 3. PRODUCTS

Base product information (shared across variants).

```java
@Entity(name = "products")
public class ProductEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String slug;

    @Column(columnDefinition = "TEXT")
    private String description;

    private boolean active = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private CategoryEntity category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sub_category_id")
    private CategoryEntity subCategory;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductVariantEntity> variants = new ArrayList<>();

    private LocalDateTime createdAt;
}
```

**Design Decisions:**

1. **Separate Category and SubCategory**
   - Allows products to be in both "Clothing" and "Men's Shirts"
   - Flexible filtering

2. **`cascade = CascadeType.ALL`**
   - When product is deleted, variants are deleted too
   - Saves manual cleanup

3. **`orphanRemoval = true`**
   - If variant is removed from list, it's deleted from DB

---

### 4. PRODUCT_VARIANTS

Specific sellable items with price and stock.

```java
@Entity(name = "product_variants")
public class ProductVariantEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private ProductEntity product;

    @Column(nullable = false, unique = true, length = 60)
    private String sku;  // Stock Keeping Unit: "SHIRT-BLU-M"

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private Integer stock;

    @Column(nullable = false, length = 30)
    private String color;

    @Enumerated(EnumType.STRING)
    private Size size;  // XS, S, M, L, XL, XXL

    private String imageUrl;
    private boolean active = true;
}
```

**Why Separate Variants from Products?**

Consider a T-shirt available in:
- 3 colors (Black, White, Blue)
- 6 sizes (XS to XXL)

That's 18 combinations! Each needs:
- Unique SKU for inventory tracking
- Separate stock count
- Potentially different prices
- Own image

**Without variants:** 18 separate products (messy!)
**With variants:** 1 product, 18 variants (clean!)

---

### 5. ORDERS

Customer purchase records.

```java
@Entity
@Table(name = "orders")
public class OrderEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItemEntity> orderItems = new ArrayList<>();

    @Column(precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    private OrderStatus status = OrderStatus.PENDING;

    private String shippingAddress;
    private String paymentMethod;
    private String paymentStatus;
    private String transactionId;  // Stripe Payment Intent ID

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

**Order Status Flow:**
```
PENDING → CREATED → PAID → SHIPPED → DELIVERED
    ↓
CANCELLED (can happen at PENDING or CREATED)
```

---

### 6. ORDER_ITEMS

Individual items within an order.

```java
@Entity
@Table(name = "order_items")
public class OrderItemEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private OrderEntity order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_variant_id", nullable = false)
    private ProductVariantEntity productVariant;

    private Integer quantity;

    @Column(precision = 10, scale = 2)
    private BigDecimal unitPrice;  // PRICE SNAPSHOT!
}
```

**Why `unitPrice` Snapshot?**

```java
// When order is placed:
item.setUnitPrice(variant.getPrice());  // $50.00

// Later, variant price changes to $60.00
// But customer only paid $50.00!

// Without snapshot: order shows $60.00 (WRONG)
// With snapshot: order shows $50.00 (CORRECT)
```

---

### 7. ADMIN_ACTIVITY_LOGS

Audit trail for admin actions.

```java
@Entity
@Table(name = "admin_activity_logs")
public class AdminActivityLogEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String action;           // CREATE, UPDATE, DELETE
    private String entityType;       // Category, Product, Order
    private Long entityId;
    private String entityName;
    private String description;      // Human-readable: "Created category 'Shirts'"
    private String changesDetail;    // What changed
    private String previousValues;   // JSON of old values
    private String newValues;        // JSON of new values
    private String adminUsername;
    private String adminDisplayName;
    private String details;
    private LocalDateTime timestamp;
    private String ipAddress;
    private String userAgent;
    private String sessionId;
}
```

**Why This Much Detail?**
- **Compliance**: Many industries require audit trails
- **Debugging**: Know who changed what and when
- **Security**: Detect unauthorized changes
- **Rollback**: `previousValues` allows reverting changes

---

## Fetch Types Explained

### `FetchType.LAZY` (Default for Collections)

```java
@ManyToOne(fetch = FetchType.LAZY)
private CategoryEntity category;
```

- Data is NOT loaded until accessed
- Reduces initial query load
- Requires active database session when accessing

### `FetchType.EAGER`

```java
@ManyToOne(fetch = FetchType.EAGER)
private CategoryEntity category;
```

- Data is loaded immediately with parent
- Can cause N+1 query problems
- Only use when data is ALWAYS needed

**Best Practice:** Use LAZY by default, EAGER only when necessary.

---

## Cascade Types

```java
@OneToMany(cascade = CascadeType.ALL)
private List<ProductVariantEntity> variants;
```

| Type | Meaning |
|------|---------|
| `PERSIST` | Save child when parent is saved |
| `MERGE` | Update child when parent is updated |
| `REMOVE` | Delete child when parent is deleted |
| `REFRESH` | Refresh child when parent is refreshed |
| `DETACH` | Detach child when parent is detached |
| `ALL` | All of the above |

---

## Indexes (Recommendations)

For better query performance, add these indexes:

```sql
-- Users
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_email ON users(email);

-- Categories
CREATE INDEX idx_categories_slug ON categories(slug);
CREATE INDEX idx_categories_parent ON categories(parent_id);

-- Products
CREATE INDEX idx_products_slug ON products(slug);
CREATE INDEX idx_products_category ON products(category_id);

-- Product Variants
CREATE INDEX idx_variants_product ON product_variants(product_id);
CREATE INDEX idx_variants_sku ON product_variants(sku);

-- Orders
CREATE INDEX idx_orders_user ON orders(user_id);
CREATE INDEX idx_orders_status ON orders(status);
CREATE INDEX idx_orders_created ON orders(created_at);

-- Order Items
CREATE INDEX idx_order_items_order ON order_items(order_id);
CREATE INDEX idx_order_items_variant ON order_items(product_variant_id);

-- Activity Logs
CREATE INDEX idx_activity_admin ON admin_activity_logs(admin_username);
CREATE INDEX idx_activity_entity ON admin_activity_logs(entity_type, entity_id);
CREATE INDEX idx_activity_timestamp ON admin_activity_logs(timestamp);
```

---

## Common Query Patterns

### Get Products with Variants

```java
// In ProductRepository
@Query("SELECT DISTINCT p FROM products p " +
       "LEFT JOIN FETCH p.variants " +
       "WHERE p.active = true")
List<ProductEntity> findAllActiveWithVariants();
```

### Get Category Tree

```java
// Get root categories with children
@Query("SELECT c FROM categories c " +
       "LEFT JOIN FETCH c.children " +
       "WHERE c.parent IS NULL")
List<CategoryEntity> findRootCategoriesWithChildren();
```

### Get Order with Items

```java
// Avoid N+1 by fetching items eagerly
@Query("SELECT o FROM OrderEntity o " +
       "LEFT JOIN FETCH o.orderItems oi " +
       "LEFT JOIN FETCH oi.productVariant " +
       "WHERE o.id = :id")
Optional<OrderEntity> findByIdWithItems(@Param("id") Long id);
```

---

## Next Steps

- Read [05-ARCHITECTURE-PATTERNS.md](./05-ARCHITECTURE-PATTERNS.md) for code organization
- Or [06-SECURITY-AUTHENTICATION.md](./06-SECURITY-AUTHENTICATION.md) for security
