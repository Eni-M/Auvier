# 16 - Technical Deep Dive

This document provides in-depth code explanations for how Auvier works internally. It covers the complete code flow from HTTP request to database and back.

---

## Table of Contents

1. [How Spring Boot Processes a Request](#1-how-spring-boot-processes-a-request)
2. [Creating a Product (Full Flow)](#2-creating-a-product-full-flow)
3. [Creating an Order (Full Flow)](#3-creating-an-order-full-flow)
4. [How MapStruct Mapping Works](#4-how-mapstruct-mapping-works)
5. [How Transactions Work](#5-how-transactions-work)
6. [How Spring Data JPA Generates Queries](#6-how-spring-data-jpa-generates-queries)
7. [How Dependency Injection Works](#7-how-dependency-injection-works)
8. [How Validation Works](#8-how-validation-works)
9. [How Security Filters Work](#9-how-security-filters-work)
10. [Common Code Patterns Explained](#10-common-code-patterns-explained)

---

## 1. How Spring Boot Processes a Request

When a user visits `/admin/products/new`, here's what happens:

```
Browser Request: GET /admin/products/new
        │
        ▼
┌─────────────────────────────────────────────────┐
│              TOMCAT WEB SERVER                   │
│  (Embedded in Spring Boot, listens on port 2525)│
└───────────────────────┬─────────────────────────┘
                        │
                        ▼
┌─────────────────────────────────────────────────┐
│           SECURITY FILTER CHAIN                  │
│  1. Check if URL requires authentication         │
│  2. Check if user has required role              │
│  3. Validate CSRF token (for POST)               │
└───────────────────────┬─────────────────────────┘
                        │
                        ▼
┌─────────────────────────────────────────────────┐
│           DISPATCHER SERVLET                     │
│  1. Find matching @Controller method             │
│  2. Resolve @PathVariable, @RequestParam         │
│  3. Call the controller method                   │
└───────────────────────┬─────────────────────────┘
                        │
                        ▼
┌─────────────────────────────────────────────────┐
│              CONTROLLER                          │
│  ProductController.newForm(Model model)          │
│  - Add data to Model                             │
│  - Return view name "admin/products/new"         │
└───────────────────────┬─────────────────────────┘
                        │
                        ▼
┌─────────────────────────────────────────────────┐
│           THYMELEAF VIEW RESOLVER                │
│  1. Find template: templates/admin/products/new.html
│  2. Process Thymeleaf expressions                │
│  3. Generate final HTML                          │
└───────────────────────┬─────────────────────────┘
                        │
                        ▼
┌─────────────────────────────────────────────────┐
│              HTTP RESPONSE                       │
│  Content-Type: text/html                         │
│  Body: <html>...</html>                          │
└─────────────────────────────────────────────────┘
```

---

## 2. Creating a Product (Full Flow)

Let's trace through what happens when an admin creates a new product.

### Step 1: User Fills Form and Submits

```html
<!-- templates/admin/products/new.html -->
<form th:action="@{/admin/products/new}" 
      th:object="${productDto}" 
      method="post">
    
    <input type="text" th:field="*{name}">
    <input type="text" th:field="*{slug}">
    <textarea th:field="*{description}"></textarea>
    
    <select th:field="*{category.id}">
        <option th:each="cat : ${categories}" 
                th:value="${cat.id}" 
                th:text="${cat.name}">
        </option>
    </select>
    
    <input type="hidden" th:name="${_csrf.parameterName}" th:value="${_csrf.token}">
    <button type="submit">Create Product</button>
</form>
```

**What `th:field="*{name}"` does:**
1. Sets `name="name"` attribute
2. Sets `id="name"` attribute  
3. Sets `value` to current value of `productDto.name`

### Step 2: HTTP Request Sent

```http
POST /admin/products/new HTTP/1.1
Content-Type: application/x-www-form-urlencoded
Cookie: JSESSIONID=abc123

name=Blue+Hoodie&slug=blue-hoodie&description=A+warm+hoodie&category.id=5&active=true&_csrf=xyz789
```

### Step 3: Controller Receives Request

```java
@Controller
@RequestMapping("/admin/products")
@RequiredArgsConstructor  // Lombok generates constructor for final fields
public class ProductController {

    private final ProductService productService;
    private final CategoryService categoryService;
    private final AdminActivityLogService activityLogService;

    // This method runs BEFORE every request to this controller
    // It adds "categories" to the Model automatically
    @ModelAttribute("categories")
    public List<CategoryDto> populateCategories() {
       return categoryService.findParentCategories();
    }

    @PostMapping("/new")
    public String create(
            @Valid @ModelAttribute("productDto") ProductDto productDto,  // Binds form data to DTO
            BindingResult bindingResult,  // Contains validation errors
            Model model) {
        
        // Handle empty category (when user selects "None")
        if (productDto.getCategory() != null && productDto.getCategory().getId() == null) {
            productDto.setCategory(null);
        }

        // Check for validation errors
        if (bindingResult.hasErrors()) {
            // Return to form with errors displayed
            return "admin/products/new";
        }

        // Call service to save product
        ProductDto created = productService.add(productDto);
        
        // Log the admin action
        activityLogService.log("CREATE", "Product", created.getId(), created.getName(), null);
        
        // Redirect to product list (PRG pattern)
        return "redirect:/admin/products";
    }
}
```

**What `@Valid` does:**
- Triggers validation annotations on `ProductDto`
- If validation fails, errors go into `BindingResult`

**What `@ModelAttribute` does:**
- Spring creates a new `ProductDto` instance
- Binds form parameters to DTO fields by name
- `name=Blue+Hoodie` → `productDto.setName("Blue Hoodie")`
- `category.id=5` → `productDto.getCategory().setId(5L)`

### Step 4: DTO Validation

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductDto {
    
    private Long id;

    @NotBlank  // Cannot be null or empty
    private String name;

    @NotBlank
    private String slug;

    private String description;  // Optional - no validation

    private boolean active = true;

    @NotNull(message = "Please select a category")
    private CategoryDto category;

    private CategoryDto subCategory;  // Optional

    private List<ProductVariantDto> variants = new ArrayList<>();
}
```

**How Validation Works:**
1. `@Valid` on controller parameter triggers validation
2. Each `@NotBlank`, `@NotNull` etc. is checked
3. Failures are collected in `BindingResult`

### Step 5: Service Layer

```java
@Service
@Transactional  // Wraps all methods in database transaction
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository repository;
    private final ProductMapper mapper;

    @Override
    public ProductDto add(ProductDto dto) {
        // Business rule: slug must be unique
        if (repository.existsBySlug(dto.getSlug())) {
            throw new DuplicateResourceException(
                "Product with slug '" + dto.getSlug() + "' already exists"
            );
        }

        // Convert DTO to Entity using MapStruct
        ProductEntity entity = mapper.toEntity(dto);
        
        // Save to database (INSERT)
        ProductEntity saved = repository.save(entity);
        
        // Convert back to DTO for return
        return mapper.toDto(saved);
    }
}
```

### Step 6: MapStruct Conversion (DTO → Entity)

```java
@Mapper(componentModel = "spring", uses = {CategoryMapper.class})
public interface ProductMapper extends BaseMapper<ProductDto, ProductEntity> {

    @Override
    @Mapping(target = "category", source = "category", qualifiedByName = "categoryDtoToEntity")
    @Mapping(target = "subCategory", source = "subCategory", qualifiedByName = "categoryDtoToEntity")
    ProductEntity toEntity(ProductDto dto);

    // Custom method to handle category conversion
    @Named("categoryDtoToEntity")
    default CategoryEntity categoryDtoToEntity(CategoryDto dto) {
        if (dto == null || dto.getId() == null) return null;
        
        // Only set ID - JPA will handle the relationship
        CategoryEntity entity = new CategoryEntity();
        entity.setId(dto.getId());
        return entity;
    }
}
```

**Generated Code (by MapStruct at compile time):**

```java
@Component
public class ProductMapperImpl implements ProductMapper {

    @Override
    public ProductEntity toEntity(ProductDto dto) {
        if (dto == null) {
            return null;
        }

        ProductEntity entity = new ProductEntity();
        
        entity.setId(dto.getId());
        entity.setName(dto.getName());
        entity.setSlug(dto.getSlug());
        entity.setDescription(dto.getDescription());
        entity.setActive(dto.isActive());
        entity.setCategory(categoryDtoToEntity(dto.getCategory()));
        entity.setSubCategory(categoryDtoToEntity(dto.getSubCategory()));
        
        return entity;
    }
}
```

### Step 7: Repository Layer (Database Save)

```java
@Repository
public interface ProductRepository extends JpaRepository<ProductEntity, Long> {
    boolean existsBySlug(String slug);
}
```

**What `repository.save(entity)` does:**

1. Hibernate inspects the entity
2. `id == null` → Generate INSERT SQL
3. `id != null` → Generate UPDATE SQL

**Generated SQL:**
```sql
INSERT INTO products (name, slug, description, active, category_id, sub_category_id, created_at)
VALUES ('Blue Hoodie', 'blue-hoodie', 'A warm hoodie', true, 5, null, '2026-02-03 10:30:00')
RETURNING id
```

### Step 8: Response Back to Browser

After `redirect:/admin/products`:

```http
HTTP/1.1 302 Found
Location: /admin/products
```

Browser follows redirect, makes new GET request, sees updated product list.

---

## 3. Creating an Order (Full Flow)

This is more complex because it involves multiple tables and inventory management.

### Step 1: Customer Checkout

```java
@Controller
@RequiredArgsConstructor
public class CheckoutController {

    private final OrderService orderService;
    private final UserService userService;

    @PostMapping("/checkout")
    public String processCheckout(
            @Valid @ModelAttribute CheckoutDto dto,
            BindingResult result,
            HttpSession session,
            Principal principal,  // Current logged-in user
            Model model) {
        
        if (result.hasErrors()) {
            return "store/checkout";
        }

        // Get cart from session
        List<CartItem> cart = getCart(session);
        
        // Get current user
        UserEntity user = userService.findByUsername(principal.getName())
            .orElseThrow();

        // Build order creation DTO
        OrderCreateDto orderDto = new OrderCreateDto();
        orderDto.setShippingAddress(dto.getShippingAddress());
        orderDto.setItems(
            cart.stream()
                .map(item -> new OrderItemCreateDto(
                    item.getVariant().getId(),
                    item.getQuantity()
                ))
                .toList()
        );

        // Create order (this is where the magic happens)
        OrderResponseDto order = orderService.createOrder(user, orderDto);

        // Clear cart
        session.removeAttribute("cart");

        return "redirect:/checkout/payment?orderId=" + order.getId();
    }
}
```

### Step 2: OrderService.createOrder - The Heart of Order Processing

```java
@Service
@Transactional  // CRUCIAL: Ensures all-or-nothing
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final InventoryService inventoryService;
    private final OrderMapper orderMapper;

    @Override
    public OrderResponseDto createOrder(UserEntity user, OrderCreateDto dto) {
        log.info("Creating order for user: {}", user.getUsername());

        // ============ STEP 1: Create Order Shell ============
        OrderEntity order = orderMapper.toEntity(dto);  // Maps shipping address
        order.setUser(user);
        order.setStatus(OrderStatus.PENDING);
        order.setPaymentStatus("PENDING");
        order.setTotalAmount(BigDecimal.ZERO);

        // Save to get generated ID
        order = orderRepository.save(order);
        
        // SQL: INSERT INTO orders (user_id, status, payment_status, total_amount, shipping_address, created_at)
        //      VALUES (1, 'PENDING', 'PENDING', 0, '123 Main St', NOW())

        // ============ STEP 2: Process Each Item ============
        BigDecimal total = BigDecimal.ZERO;
        
        for (OrderItemCreateDto itemDto : dto.getItems()) {
            Long variantId = itemDto.getProductVariantId();
            int quantity = itemDto.getQuantity();

            // Check if enough stock exists
            // Throws exception if not enough → Transaction rolls back
            inventoryService.validateStock(variantId, quantity);
            
            // Reduce stock in database
            inventoryService.reserveStock(variantId, quantity);
            // SQL: UPDATE product_variants SET stock = stock - 2 WHERE id = 10

            // Get variant for price info
            ProductVariantEntity variant = inventoryService.getVariant(variantId);

            // Create order item
            OrderItemEntity item = new OrderItemEntity();
            item.setOrder(order);
            item.setProductVariant(variant);
            item.setQuantity(quantity);
            item.setUnitPrice(variant.getPrice());  // SNAPSHOT the price!

            // Add to order's item list
            order.addOrderItem(item);
            // This calls: orderItems.add(item); item.setOrder(this);

            // Accumulate total
            total = total.add(item.getSubtotal());
            // getSubtotal() = unitPrice * quantity
        }

        // ============ STEP 3: Save Final Order ============
        order.setTotalAmount(total);
        order = orderRepository.save(order);
        
        // SQL: UPDATE orders SET total_amount = 149.98 WHERE id = 123
        // SQL: INSERT INTO order_items (order_id, product_variant_id, quantity, unit_price)
        //      VALUES (123, 10, 2, 49.99), (123, 15, 1, 49.99)

        log.info("Order created successfully. ID: {}, Total: {}", order.getId(), total);
        
        return orderMapper.toResponseDto(order);
    }
}
```

### Step 3: Inventory Management

```java
@Service
@Transactional
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private final ProductVariantRepository variantRepository;

    @Override
    public void validateStock(Long variantId, int quantity) {
        ProductVariantEntity variant = getVariant(variantId);

        // Check if variant is active
        if (!variant.isActive()) {
            throw new IllegalStateException(
                "Product variant '" + variant.getSku() + "' is not available"
            );
        }

        // Check if enough stock
        if (variant.getStock() < quantity) {
            throw new IllegalStateException(
                "Insufficient stock for '" + variant.getSku() + "'. " +
                "Available: " + variant.getStock() + ", Requested: " + quantity
            );
        }
    }

    @Override
    public void reserveStock(Long variantId, int quantity) {
        // Validate first (double-check)
        validateStock(variantId, quantity);
        
        ProductVariantEntity variant = getVariant(variantId);
        
        // Decrease stock
        variant.setStock(variant.getStock() - quantity);
        
        // Save change
        variantRepository.save(variant);
        // SQL: UPDATE product_variants SET stock = 98 WHERE id = 10
    }

    @Override
    public void releaseStock(Long variantId, int quantity) {
        // Used when order is cancelled
        ProductVariantEntity variant = getVariant(variantId);
        variant.setStock(variant.getStock() + quantity);
        variantRepository.save(variant);
    }

    @Override
    public ProductVariantEntity getVariant(Long variantId) {
        return variantRepository.findById(variantId)
            .orElseThrow(() -> new ResourceNotFoundException("ProductVariant", variantId));
    }
}
```

### What Happens If Something Fails?

```java
// Scenario: User orders 2 items, but second item has no stock

createOrder(user, dto) {
    order = repository.save(order);  // ✓ Saved
    
    // Item 1
    inventoryService.validateStock(item1);  // ✓ OK
    inventoryService.reserveStock(item1);   // ✓ Stock reduced
    
    // Item 2
    inventoryService.validateStock(item2);  // ✗ THROWS EXCEPTION!
    // "Insufficient stock for 'SHIRT-BLU-S'"
}

// Because of @Transactional:
// - Order INSERT is ROLLED BACK
// - Item 1 stock reduction is ROLLED BACK
// - Database returns to original state
// - Exception bubbles up to controller
// - Error page shown to user
```

---

## 4. How MapStruct Mapping Works

### Basic Mapping

```java
// Interface you write
@Mapper(componentModel = "spring")
public interface CategoryMapper {
    CategoryDto toDto(CategoryEntity entity);
    CategoryEntity toEntity(CategoryDto dto);
}
```

**MapStruct generates at compile time:**

```java
@Component
public class CategoryMapperImpl implements CategoryMapper {

    @Override
    public CategoryDto toDto(CategoryEntity entity) {
        if (entity == null) {
            return null;
        }

        CategoryDto dto = new CategoryDto();
        
        // Maps fields with same name automatically
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setSlug(entity.getSlug());
        dto.setDescription(entity.getDescription());
        dto.setActive(entity.isActive());
        
        return dto;
    }

    @Override
    public CategoryEntity toEntity(CategoryDto dto) {
        if (dto == null) {
            return null;
        }

        CategoryEntity entity = new CategoryEntity();
        
        entity.setId(dto.getId());
        entity.setName(dto.getName());
        entity.setSlug(dto.getSlug());
        entity.setDescription(dto.getDescription());
        entity.setActive(dto.isActive());
        
        return entity;
    }
}
```

### Complex Mapping with Custom Methods

```java
@Mapper(componentModel = "spring")
public interface OrderMapper {

    // Maps OrderItemEntity to OrderItemResponseDto
    @Mapping(target = "productName", source = "productVariant.product.name")
    @Mapping(target = "variantName", expression = "java(buildVariantName(item.getProductVariant()))")
    @Mapping(target = "subtotal", expression = "java(calculateSubtotal(item))")
    OrderItemResponseDto toItemResponseDto(OrderItemEntity item);

    // Custom method called by expression
    default String buildVariantName(ProductVariantEntity variant) {
        StringBuilder sb = new StringBuilder();
        if (variant.getColor() != null) {
            sb.append(variant.getColor());
        }
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

**Generated code:**

```java
@Override
public OrderItemResponseDto toItemResponseDto(OrderItemEntity item) {
    if (item == null) {
        return null;
    }

    OrderItemResponseDto dto = new OrderItemResponseDto();
    
    // Nested property access
    dto.setProductName(item.getProductVariant().getProduct().getName());
    
    // Custom expression calls your default method
    dto.setVariantName(buildVariantName(item.getProductVariant()));
    
    // Another custom expression
    dto.setSubtotal(calculateSubtotal(item));
    
    // Regular mappings
    dto.setId(item.getId());
    dto.setQuantity(item.getQuantity());
    dto.setUnitPrice(item.getUnitPrice());
    
    return dto;
}
```

### Updating Existing Entity

```java
@Mapper(componentModel = "spring")
public interface ProductMapper {

    // Updates existing entity instead of creating new one
    @Mapping(target = "id", ignore = true)        // Don't overwrite ID
    @Mapping(target = "createdAt", ignore = true) // Don't overwrite audit field
    void updateEntityFromDto(ProductDto dto, @MappingTarget ProductEntity entity);
}

// Usage in service:
public ProductDto modify(Long id, ProductDto dto) {
    ProductEntity existing = repository.findById(id).orElseThrow();
    
    mapper.updateEntityFromDto(dto, existing);  // Updates existing, doesn't create new
    
    return mapper.toDto(repository.save(existing));
}
```

---

## 5. How Transactions Work

### The @Transactional Annotation

```java
@Service
@Transactional  // Applied to all methods in class
public class OrderServiceImpl {

    public OrderResponseDto createOrder(...) {
        // Everything in here is ONE transaction
        
        order = orderRepository.save(order);      // Not committed yet
        inventoryService.reserveStock(...);       // Not committed yet
        orderItemRepository.save(item);           // Not committed yet
        
        // If we reach here without exception:
        // → All changes are COMMITTED together
        
        // If any exception is thrown:
        // → All changes are ROLLED BACK
    }
}
```

### Transaction Boundaries

```java
@Service
@Transactional
public class OrderServiceImpl {

    // Same transaction (called within @Transactional method)
    private final InventoryService inventoryService;

    public OrderResponseDto createOrder(...) {
        // START TRANSACTION
        
        orderRepository.save(order);
        
        inventoryService.reserveStock(...);  // Uses SAME transaction
        // Because InventoryService is also @Transactional,
        // but it joins the existing transaction by default
        
        // END TRANSACTION (commit or rollback)
    }
}
```

### Manual Transaction Control (Rare)

```java
@Service
public class SomeService {

    @Autowired
    private TransactionTemplate transactionTemplate;

    public void doSomethingComplex() {
        // Manual transaction
        transactionTemplate.execute(status -> {
            // Do work
            if (somethingWrong) {
                status.setRollbackOnly();  // Mark for rollback
            }
            return result;
        });
    }
}
```

---

## 6. How Spring Data JPA Generates Queries

### Method Name → SQL

```java
public interface ProductRepository extends JpaRepository<ProductEntity, Long> {
    
    // Spring parses method name and generates query
    
    Optional<ProductEntity> findBySlug(String slug);
    // SQL: SELECT * FROM products WHERE slug = ?
    
    List<ProductEntity> findByActiveTrue();
    // SQL: SELECT * FROM products WHERE active = true
    
    List<ProductEntity> findByCategoryIdAndActiveTrue(Long categoryId);
    // SQL: SELECT * FROM products WHERE category_id = ? AND active = true
    
    List<ProductEntity> findByNameContaining(String name);
    // SQL: SELECT * FROM products WHERE name LIKE '%' || ? || '%'
    
    List<ProductEntity> findByPriceBetween(BigDecimal min, BigDecimal max);
    // SQL: SELECT * FROM products WHERE price BETWEEN ? AND ?
    
    List<ProductEntity> findTop5ByOrderByCreatedAtDesc();
    // SQL: SELECT * FROM products ORDER BY created_at DESC LIMIT 5
    
    boolean existsBySlug(String slug);
    // SQL: SELECT CASE WHEN COUNT(*) > 0 THEN true ELSE false END 
    //      FROM products WHERE slug = ?
    
    long countByActiveTrue();
    // SQL: SELECT COUNT(*) FROM products WHERE active = true
}
```

### Custom JPQL Queries

```java
public interface OrderRepository extends JpaRepository<OrderEntity, Long> {

    // JPQL (Java Persistence Query Language)
    @Query("SELECT o FROM OrderEntity o WHERE o.user.id = :userId ORDER BY o.createdAt DESC")
    List<OrderEntity> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);
    
    // With JOIN FETCH to avoid N+1 problem
    @Query("SELECT o FROM OrderEntity o " +
           "LEFT JOIN FETCH o.orderItems oi " +
           "LEFT JOIN FETCH oi.productVariant pv " +
           "WHERE o.id = :id")
    Optional<OrderEntity> findByIdWithItems(@Param("id") Long id);
}
```

### Native SQL Queries

```java
public interface ProductRepository extends JpaRepository<ProductEntity, Long> {

    // When you need database-specific features
    @Query(value = "SELECT * FROM products " +
                   "WHERE to_tsvector('english', name || ' ' || COALESCE(description, '')) " +
                   "@@ plainto_tsquery('english', :query)",
           nativeQuery = true)
    List<ProductEntity> fullTextSearch(@Param("query") String query);
}
```

---

## 7. How Dependency Injection Works

### Constructor Injection (Recommended)

```java
@Service
@RequiredArgsConstructor  // Lombok generates constructor
public class ProductServiceImpl implements ProductService {

    // These are injected via constructor
    private final ProductRepository repository;
    private final ProductMapper mapper;
    
    // Lombok generates:
    // public ProductServiceImpl(ProductRepository repository, ProductMapper mapper) {
    //     this.repository = repository;
    //     this.mapper = mapper;
    // }
}
```

### How Spring Finds Dependencies

```
1. Spring starts up
2. Scans for @Component, @Service, @Repository, @Controller
3. Creates instances (beans) of each
4. For ProductServiceImpl:
   - Needs ProductRepository → Find bean of that type
   - Needs ProductMapper → Find bean of that type (created by MapStruct)
   - Inject both via constructor
5. Store ProductServiceImpl bean in ApplicationContext
6. Later, when Controller needs ProductService:
   - Find ProductServiceImpl bean
   - Inject it
```

### Bean Lifecycle

```java
@Service
public class SomeService {

    @PostConstruct  // Called after injection
    public void init() {
        // Initialize something
    }

    @PreDestroy  // Called before shutdown
    public void cleanup() {
        // Clean up resources
    }
}
```

---

## 8. How Validation Works

### Validation Annotations

```java
public class UserRegistrationDto {

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be 3-50 characters")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "Only letters, numbers, underscore")
    private String username;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;
}
```

### Validation in Controller

```java
@PostMapping("/register")
public String register(
        @Valid @ModelAttribute UserRegistrationDto dto,  // @Valid triggers validation
        BindingResult result) {                          // Errors go here
    
    if (result.hasErrors()) {
        // result.getAllErrors() - all errors
        // result.getFieldErrors() - field-specific errors
        // result.getFieldError("username") - error for specific field
        return "auth/register";
    }
    
    // Valid data - proceed
}
```

### Custom Validator

```java
// Custom annotation
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = UniqueUsernameValidator.class)
public @interface UniqueUsername {
    String message() default "Username already exists";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}

// Validator implementation
@Component
public class UniqueUsernameValidator implements ConstraintValidator<UniqueUsername, String> {

    @Autowired
    private UserRepository userRepository;

    @Override
    public boolean isValid(String username, ConstraintValidatorContext context) {
        if (username == null) return true;  // Let @NotBlank handle null
        return !userRepository.existsByUsername(username);
    }
}

// Usage
public class UserRegistrationDto {
    @UniqueUsername
    private String username;
}
```

---

## 9. How Security Filters Work

### Filter Chain

```
Request → [Filter 1] → [Filter 2] → ... → [Controller]
                                              ↓
Response ← [Filter 1] ← [Filter 2] ← ... ← [Controller]
```

### Spring Security Filter Chain

```java
// SecurityConfig creates this chain:
http.authorizeHttpRequests(...)
    .formLogin(...)
    .logout(...);

// Results in these filters (simplified):
1. SecurityContextPersistenceFilter
   - Loads SecurityContext from session
   - Makes it available via SecurityContextHolder

2. CsrfFilter
   - Validates CSRF token on POST/PUT/DELETE

3. UsernamePasswordAuthenticationFilter
   - Handles POST /login
   - Creates Authentication object on success

4. AnonymousAuthenticationFilter
   - If not authenticated, sets anonymous principal

5. AuthorizationFilter
   - Checks if request is allowed based on URL rules
   - Throws AccessDeniedException if not
```

### Authentication Flow

```java
// When user POSTs to /login:

UsernamePasswordAuthenticationFilter:
    1. Extract username/password from request
    2. Create UsernamePasswordAuthenticationToken(username, password)
    3. Call AuthenticationManager.authenticate(token)
    
AuthenticationManager:
    1. Find AuthenticationProvider that handles this token
    2. DaoAuthenticationProvider is used
    
DaoAuthenticationProvider:
    1. Call UserDetailsService.loadUserByUsername(username)
    2. Your UserServiceImpl returns UserDetails
    3. Compare password with BCrypt
    4. If match: Create authenticated Authentication object
    5. If not: Throw BadCredentialsException
    
Back to Filter:
    1. Store Authentication in SecurityContextHolder
    2. Call successHandler.onAuthenticationSuccess()
    3. Your handler redirects to /admin or /
```

### Getting Current User

```java
// Option 1: In Controller
@GetMapping("/profile")
public String profile(Principal principal) {
    String username = principal.getName();
}

// Option 2: In Service
public void someMethod() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    String username = auth.getName();
    
    // Get authorities
    Collection<? extends GrantedAuthority> authorities = auth.getAuthorities();
    boolean isAdmin = authorities.stream()
        .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
}
```

---

## 10. Common Code Patterns Explained

### Pattern 1: PRG (Post-Redirect-Get)

```java
@PostMapping("/new")
public String create(...) {
    // Process form
    service.save(entity);
    
    // DON'T return view directly
    // return "products/list";  // BAD - refresh would resubmit
    
    // DO redirect
    return "redirect:/admin/products";  // GOOD - safe to refresh
}
```

### Pattern 2: Flash Attributes

```java
@PostMapping("/delete")
public String delete(Long id, RedirectAttributes flash) {
    service.delete(id);
    
    // Add message that survives redirect
    flash.addFlashAttribute("success", "Deleted successfully!");
    
    return "redirect:/admin/products";
}

// In template:
// <div th:if="${success}" th:text="${success}"></div>
```

### Pattern 3: Optional Handling

```java
// Bad - can throw NoSuchElementException
ProductEntity entity = repository.findById(id).get();

// Good - explicit error handling
ProductEntity entity = repository.findById(id)
    .orElseThrow(() -> new ResourceNotFoundException("Product", id));

// Good - return empty if not found
Optional<ProductDto> dto = repository.findById(id)
    .map(mapper::toDto);
```

### Pattern 4: Stream Processing

```java
// Convert list of entities to DTOs
List<ProductDto> dtos = entities.stream()
    .map(mapper::toDto)
    .toList();

// Filter and collect
List<ProductDto> activeProducts = products.stream()
    .filter(ProductDto::isActive)
    .filter(p -> p.getStock() > 0)
    .sorted(Comparator.comparing(ProductDto::getName))
    .toList();

// Calculate total
BigDecimal total = items.stream()
    .map(OrderItemEntity::getSubtotal)
    .reduce(BigDecimal.ZERO, BigDecimal::add);
```

### Pattern 5: Builder Pattern with Lombok

```java
// With @Builder on class
OrderEntity order = OrderEntity.builder()
    .user(user)
    .status(OrderStatus.PENDING)
    .totalAmount(BigDecimal.ZERO)
    .shippingAddress(address)
    .build();
```

### Pattern 6: Null-Safe Navigation

```java
// Unsafe
String categoryName = product.getCategory().getName();  // NPE if category is null

// Safe with Optional
String categoryName = Optional.ofNullable(product.getCategory())
    .map(CategoryEntity::getName)
    .orElse("Uncategorized");

// Safe with ternary
String categoryName = product.getCategory() != null 
    ? product.getCategory().getName() 
    : "Uncategorized";
```

---

## Quick Reference: Key Annotations

| Annotation | Purpose |
|------------|---------|
| `@Controller` | Marks class as web controller |
| `@Service` | Marks class as service (business logic) |
| `@Repository` | Marks class as data access |
| `@Component` | Generic Spring-managed bean |
| `@Autowired` | Inject dependency (prefer constructor) |
| `@RequiredArgsConstructor` | Lombok: generate constructor for final fields |
| `@Transactional` | Wrap method in database transaction |
| `@Valid` | Trigger validation |
| `@GetMapping` | Handle GET requests |
| `@PostMapping` | Handle POST requests |
| `@PathVariable` | Extract value from URL path |
| `@RequestParam` | Extract query parameter |
| `@ModelAttribute` | Bind form data to object |
| `@Mapper` | MapStruct: generate mapping code |
| `@Entity` | JPA: maps class to database table |
| `@Id` | JPA: primary key field |
| `@ManyToOne` | JPA: foreign key relationship |
| `@OneToMany` | JPA: inverse relationship |

---

This document should give you a deep understanding of how the code works internally. For more context, refer to the other documentation files in the Project Info folder.
