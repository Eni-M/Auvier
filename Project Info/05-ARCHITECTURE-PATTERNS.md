# 05 - Architecture Patterns

This document explains the software design patterns and architectural decisions used in Auvier.

---

## Layered Architecture

The application follows a strict layered architecture where each layer has a specific responsibility and only communicates with adjacent layers.

```
┌──────────────────────────────────────────────────────────────────┐
│                     PRESENTATION LAYER                            │
│  ┌────────────────┐  ┌────────────────┐  ┌────────────────────┐  │
│  │  Controllers   │  │   Templates    │  │   Static Assets   │  │
│  │  (@Controller) │  │  (Thymeleaf)   │  │   (CSS, JS)       │  │
│  └───────┬────────┘  └────────────────┘  └────────────────────┘  │
│          │ Uses DTOs                                              │
│          ▼                                                        │
├──────────────────────────────────────────────────────────────────┤
│                      SERVICE LAYER                                │
│  ┌────────────────────────────────────────────────────────────┐  │
│  │  Business Logic (@Service)                                  │  │
│  │  - Validation rules                                         │  │
│  │  - Transaction management                                   │  │
│  │  - Orchestration                                            │  │
│  └───────┬────────────────────────────────────────────────────┘  │
│          │ Uses Entities                                          │
│          ▼                                                        │
├──────────────────────────────────────────────────────────────────┤
│                    REPOSITORY LAYER                               │
│  ┌────────────────────────────────────────────────────────────┐  │
│  │  Data Access (@Repository)                                  │  │
│  │  - Spring Data JPA interfaces                               │  │
│  │  - Custom queries                                           │  │
│  └───────┬────────────────────────────────────────────────────┘  │
│          │                                                        │
│          ▼                                                        │
├──────────────────────────────────────────────────────────────────┤
│                     DATABASE LAYER                                │
│  ┌────────────────────────────────────────────────────────────┐  │
│  │  PostgreSQL                                                 │  │
│  │  - Tables mapped from JPA entities                          │  │
│  └────────────────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────────┘
```

### Why This Separation?

| Benefit | Explanation |
|---------|-------------|
| **Testability** | Mock services to test controllers; mock repositories to test services |
| **Maintainability** | Change database without touching controllers |
| **Team Collaboration** | Frontend dev works on controllers; backend on services |
| **Reusability** | Same service can be used by web controller AND REST API |

---

## Pattern 1: DTO Pattern (Data Transfer Object)

### The Problem

```java
// ❌ BAD: Exposing entity to view
@GetMapping("/users/{id}")
public UserEntity getUser(@PathVariable Long id) {
    return userRepository.findById(id).orElseThrow();
}
```

Problems:
1. **Security**: Exposes password hash
2. **Coupling**: View depends on entity structure
3. **Performance**: May load unnecessary relations
4. **Validation**: Entity validation != form validation

### The Solution

```java
// DTO for displaying user info
public class UserResponseDto {
    private Long id;
    private String username;
    private String email;
    private String fullName;
    // No password field!
}

// DTO for user registration
public class UserRegistrationDto {
    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50)
    private String username;

    @NotBlank
    @Email
    private String email;

    @NotBlank
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;

    @NotBlank
    private String confirmPassword;  // Not in entity!
}

// ✅ GOOD: Controller uses DTOs
@GetMapping("/users/{id}")
public UserResponseDto getUser(@PathVariable Long id) {
    return userService.findById(id);
}

@PostMapping("/register")
public String register(@Valid @ModelAttribute UserRegistrationDto dto) {
    userService.registerUser(dto);
    return "redirect:/login";
}
```

### DTO Types in Auvier

| DTO Type | Purpose | Example |
|----------|---------|---------|
| `*Dto` | Create/Update | `CategoryDto`, `ProductDto` |
| `*ResponseDto` | Read (full details) | `UserResponseDto`, `OrderResponseDto` |
| `*SummaryDto` | Read (list item) | `UserSummaryDto`, `OrderSummaryDto` |
| `*CreateDto` | Create only | `OrderCreateDto`, `OrderItemCreateDto` |

---

## Pattern 2: Mapper Pattern (with MapStruct)

### The Problem

Manual mapping is tedious and error-prone:

```java
// ❌ BAD: Manual mapping
public UserResponseDto toDto(UserEntity entity) {
    UserResponseDto dto = new UserResponseDto();
    dto.setId(entity.getId());
    dto.setUsername(entity.getUsername());
    dto.setEmail(entity.getEmail());
    dto.setFirstName(entity.getFirstName());
    dto.setLastName(entity.getLastName());
    // 10 more fields...
    // What if you forget one?
    return dto;
}
```

### The Solution: MapStruct

```java
@Mapper(componentModel = "spring")
public interface UserMapper {

    // Simple mapping (fields match by name)
    UserResponseDto toResponseDto(UserEntity entity);

    // List mapping
    List<UserSummaryDto> toDtoList(List<UserEntity> entities);

    // Custom mapping for complex fields
    @Mapping(target = "fullName", expression = "java(entity.getFirstName() + ' ' + entity.getLastName())")
    UserSummaryDto toSummaryDto(UserEntity entity);

    // Ignore certain fields
    @Mapping(target = "password", ignore = true)
    UserEntity toEntity(UserRegistrationDto dto);

    // Update existing entity from DTO
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "password", ignore = true)
    void updateEntityFromDto(UserRegistrationDto dto, @MappingTarget UserEntity entity);
}
```

MapStruct generates implementation at compile time:

```java
// Generated in target/generated-sources/annotations/
@Component
public class UserMapperImpl implements UserMapper {

    @Override
    public UserResponseDto toResponseDto(UserEntity entity) {
        if (entity == null) {
            return null;
        }
        UserResponseDto dto = new UserResponseDto();
        dto.setId(entity.getId());
        dto.setUsername(entity.getUsername());
        dto.setEmail(entity.getEmail());
        dto.setFirstName(entity.getFirstName());
        dto.setLastName(entity.getLastName());
        return dto;
    }
    // ... more methods
}
```

### Why MapStruct?

| Feature | MapStruct | Manual | ModelMapper |
|---------|-----------|--------|-------------|
| Type Safety | ✅ Compile-time | ❌ Runtime | ❌ Runtime |
| Performance | ✅ Direct field access | ✅ Direct | ❌ Reflection |
| IDE Support | ✅ Full | ✅ Full | ⚠️ Limited |
| Boilerplate | ✅ None | ❌ Lots | ✅ None |

---

## Pattern 3: Service Pattern

### The Problem

Controllers doing too much:

```java
// ❌ BAD: Fat controller
@PostMapping("/orders")
public String createOrder(@ModelAttribute OrderDto dto, Principal principal) {
    // Validation
    if (dto.getItems().isEmpty()) {
        throw new IllegalArgumentException("Order must have items");
    }

    // Get user
    User user = userRepository.findByUsername(principal.getName());

    // Check stock for each item
    for (OrderItemDto item : dto.getItems()) {
        ProductVariant variant = variantRepository.findById(item.getVariantId());
        if (variant.getStock() < item.getQuantity()) {
            throw new InsufficientStockException();
        }
    }

    // Create order
    Order order = new Order();
    order.setUser(user);
    // ... 50 more lines of logic
    orderRepository.save(order);

    // Send email
    emailService.sendOrderConfirmation(order);

    return "redirect:/orders/" + order.getId();
}
```

### The Solution: Service Layer

```java
// Controller is thin
@Controller
@RequiredArgsConstructor
public class CheckoutController {

    private final OrderService orderService;

    @PostMapping("/orders")
    public String createOrder(@ModelAttribute OrderCreateDto dto, Principal principal) {
        UserEntity user = getCurrentUser(principal);
        OrderResponseDto order = orderService.createOrder(user, dto);
        return "redirect:/orders/" + order.getId();
    }
}

// Service contains business logic
@Service
@Transactional
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final InventoryService inventoryService;
    private final OrderMapper orderMapper;

    @Override
    public OrderResponseDto createOrder(UserEntity user, OrderCreateDto dto) {
        // All business logic here
        OrderEntity order = new OrderEntity();
        order.setUser(user);
        order.setStatus(OrderStatus.PENDING);

        BigDecimal total = BigDecimal.ZERO;
        for (OrderItemCreateDto itemDto : dto.getItems()) {
            // Validate and reserve stock
            inventoryService.validateStock(itemDto.getVariantId(), itemDto.getQuantity());
            inventoryService.reserveStock(itemDto.getVariantId(), itemDto.getQuantity());

            // Create item
            OrderItemEntity item = createOrderItem(itemDto);
            order.addOrderItem(item);
            total = total.add(item.getSubtotal());
        }

        order.setTotalAmount(total);
        order = orderRepository.save(order);

        return orderMapper.toResponseDto(order);
    }
}
```

### Service Responsibilities

| Responsibility | Example |
|----------------|---------|
| Validation | Check stock before order |
| Calculation | Calculate order total |
| Transaction | All-or-nothing database updates |
| Orchestration | Call inventory, payment, email services |
| Security | Verify user owns resource |

---

## Pattern 4: Repository Pattern

### Spring Data JPA Magic

```java
// Just an interface - Spring generates implementation!
public interface ProductRepository extends JpaRepository<ProductEntity, Long> {

    // Method name → SQL query (automatic!)
    Optional<ProductEntity> findBySlug(String slug);

    List<ProductEntity> findByActiveTrue();

    List<ProductEntity> findByCategoryIdAndActiveTrue(Long categoryId);

    boolean existsBySlug(String slug);

    // Custom JPQL when needed
    @Query("SELECT p FROM products p WHERE p.name LIKE %:search% OR p.description LIKE %:search%")
    List<ProductEntity> search(@Param("search") String search);

    // Native SQL for complex queries
    @Query(value = "SELECT * FROM products WHERE created_at > NOW() - INTERVAL '7 days'",
           nativeQuery = true)
    List<ProductEntity> findRecentProducts();
}
```

### Query Method Keywords

| Keyword | Sample | JPQL |
|---------|--------|------|
| `findBy` | `findByName(name)` | `WHERE name = ?` |
| `findAllBy` | `findAllByActive(true)` | `WHERE active = ?` |
| `And` | `findByNameAndColor(n, c)` | `WHERE name = ? AND color = ?` |
| `Or` | `findByNameOrSlug(n, s)` | `WHERE name = ? OR slug = ?` |
| `Between` | `findByPriceBetween(a, b)` | `WHERE price BETWEEN ? AND ?` |
| `LessThan` | `findByStockLessThan(n)` | `WHERE stock < ?` |
| `GreaterThan` | `findByPriceGreaterThan(p)` | `WHERE price > ?` |
| `Like` | `findByNameLike(pattern)` | `WHERE name LIKE ?` |
| `Containing` | `findByNameContaining(s)` | `WHERE name LIKE %?%` |
| `OrderBy` | `findAllByOrderByCreatedAtDesc()` | `ORDER BY created_at DESC` |
| `Top`/`First` | `findTop5ByOrderByPrice()` | `LIMIT 5` |

---

## Pattern 5: Generic Service Pattern

### The Problem: Repetitive CRUD

```java
// CategoryService
public CategoryDto findById(Long id) { ... }
public List<CategoryDto> findAll() { ... }
public CategoryDto add(CategoryDto dto) { ... }
public CategoryDto update(Long id, CategoryDto dto) { ... }
public void delete(Long id) { ... }

// ProductService - same methods!
public ProductDto findById(Long id) { ... }
public List<ProductDto> findAll() { ... }
// etc...
```

### The Solution: Generic Interface

```java
public interface GenericService<DTO, ID> {
    Optional<DTO> findOne(ID id);
    List<DTO> findAll();
    DTO add(DTO dto);
    DTO update(ID id, DTO dto);
    void delete(ID id);
}

// CategoryService extends with category-specific methods
public interface CategoryService extends GenericService<CategoryDto, Long> {
    List<CategoryDto> findRootCategories();
    List<CategoryDto> findByParentId(Long parentId);
}
```

---

## Pattern 6: Builder Pattern (with Lombok)

### Without Builder

```java
// ❌ Tedious constructor or setters
ProductDto dto = new ProductDto();
dto.setName("Shirt");
dto.setSlug("shirt");
dto.setDescription("A nice shirt");
dto.setActive(true);
dto.setCategoryId(1L);
```

### With Lombok @Builder

```java
@Data
@Builder
public class ProductDto {
    private Long id;
    private String name;
    private String slug;
    private String description;
    private boolean active;
    private Long categoryId;
}

// ✅ Clean and readable
ProductDto dto = ProductDto.builder()
    .name("Shirt")
    .slug("shirt")
    .description("A nice shirt")
    .active(true)
    .categoryId(1L)
    .build();
```

---

## Pattern 7: Dependency Injection

### Constructor Injection (Preferred)

```java
@Service
@RequiredArgsConstructor  // Lombok generates constructor
public class OrderServiceImpl implements OrderService {

    // final fields are injected via constructor
    private final OrderRepository orderRepository;
    private final InventoryService inventoryService;
    private final PaymentService paymentService;
    private final OrderMapper orderMapper;

    // No @Autowired needed!
}
```

### Why Constructor Injection?

| Benefit | Explanation |
|---------|-------------|
| **Immutability** | Fields are `final`, can't be changed |
| **Required Dependencies** | App won't start if dependency missing |
| **Testability** | Easy to provide mocks in tests |
| **No Reflection** | Faster than field injection |

---

## Pattern 8: Exception Handling

### Custom Exceptions

```java
// Base exception
public class AuvierException extends RuntimeException {
    public AuvierException(String message) {
        super(message);
    }
}

// Specific exceptions
public class ResourceNotFoundException extends AuvierException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}

public class DuplicateResourceException extends AuvierException {
    public DuplicateResourceException(String message) {
        super(message);
    }
}

public class InsufficientStockException extends AuvierException {
    public InsufficientStockException(String message) {
        super(message);
    }
}
```

### Global Exception Handler

```java
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public String handleNotFound(ResourceNotFoundException ex, Model model) {
        model.addAttribute("message", ex.getMessage());
        return "error/404";
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public String handleDuplicate(DuplicateResourceException ex, RedirectAttributes flash) {
        flash.addFlashAttribute("error", ex.getMessage());
        return "redirect:/admin";
    }
}
```

---

## Summary: Data Flow

Here's how data flows through the application:

```
HTTP Request
     │
     ▼
┌─────────────┐
│ Controller  │ ← Receives DTO from form/JSON
│             │ ← Validates with @Valid
└──────┬──────┘
       │ Calls service with DTO
       ▼
┌─────────────┐
│  Service    │ ← Business logic
│             │ ← Uses Mapper to convert DTO → Entity
└──────┬──────┘
       │ Calls repository with Entity
       ▼
┌─────────────┐
│ Repository  │ ← Saves/retrieves Entity
└──────┬──────┘
       │ Returns Entity
       ▼
┌─────────────┐
│  Service    │ ← Uses Mapper to convert Entity → DTO
└──────┬──────┘
       │ Returns DTO
       ▼
┌─────────────┐
│ Controller  │ ← Adds DTO to Model
│             │ ← Returns view name
└──────┬──────┘
       │
       ▼
┌─────────────┐
│  Thymeleaf  │ ← Renders HTML with DTO data
└─────────────┘
       │
       ▼
HTTP Response (HTML)
```

---

## Next Steps

- Read [06-SECURITY-AUTHENTICATION.md](./06-SECURITY-AUTHENTICATION.md) for security details
- Or [07-ADMIN-PANEL.md](./07-ADMIN-PANEL.md) for admin features
