# 03 - Project Structure

This document explains every folder and file in the Auvier project.

---

## Root Directory

```
Auvier/
├── src/                    # Source code
├── target/                 # Compiled output (generated)
├── uploads/                # User-uploaded files
├── logs/                   # Application logs
├── docs/                   # Additional documentation
├── Project Info/           # This documentation
├── compose.yaml            # Docker Compose for PostgreSQL
├── pom.xml                 # Maven configuration
├── mvnw, mvnw.cmd         # Maven wrapper scripts
├── README.md              # Quick start guide
└── HELP.md                # Spring Boot help
```

---

## Source Code Structure

```
src/
├── main/
│   ├── java/com/auvier/       # Java source code
│   └── resources/             # Configuration and templates
└── test/
    ├── java/com/auvier/       # Test classes
    └── resources/             # Test configuration
```

---

## Java Package Structure

```
src/main/java/com/auvier/
├── AuvierApplication.java     # Main entry point
├── config/                    # Configuration classes
├── controllers/               # HTTP request handlers
│   └── admin/                # Admin-specific controllers
├── dtos/                      # Data Transfer Objects
│   ├── errors/               # Error response DTOs
│   ├── order/                # Order-related DTOs
│   └── user/                 # User-related DTOs
├── entities/                  # JPA database entities
│   └── catalog/              # Product/Category entities
├── enums/                     # Enumeration types
├── exception/                 # Custom exceptions
├── infrastructure/            # Business logic
│   ├── genericservices/      # Base service classes
│   └── services/             # Service interfaces
│       └── impl/             # Service implementations
├── mappers/                   # Entity ↔ DTO mappers
└── repositories/              # Database access
```

---

## Detailed Breakdown

### 1. `AuvierApplication.java`

The main entry point of the application:

```java
@SpringBootApplication
public class AuvierApplication {
    public static void main(String[] args) {
        SpringApplication.run(AuvierApplication.class, args);
    }
}
```

**What `@SpringBootApplication` does:**
- `@Configuration`: Marks class as configuration source
- `@EnableAutoConfiguration`: Auto-configures Spring beans
- `@ComponentScan`: Scans for components in this package and sub-packages

---

### 2. `config/` - Configuration Classes

| File | Purpose |
|------|---------|
| `SecurityConfig.java` | Spring Security setup (authentication, authorization) |
| `PasswordConfig.java` | BCrypt password encoder bean |
| `DataInitializer.java` | Creates default admin user on startup |
| `StripeConfig.java` | Stripe API key configuration |
| `WebMvcConfig.java` | Custom resource handlers (for `/uploads/`) |

#### SecurityConfig.java Explained

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) {
        http
            // Define URL access rules
            .authorizeHttpRequests(auth -> auth
                // Public resources (CSS, JS, images)
                .requestMatchers("/assets/**", "/uploads/**").permitAll()
                
                // Public pages
                .requestMatchers("/", "/shop/**", "/about").permitAll()
                
                // Login/Register only for non-authenticated
                .requestMatchers("/login", "/register").anonymous()
                
                // Admin area requires ADMIN role
                .requestMatchers("/admin/**").hasRole("ADMIN")
                
                // Everything else requires authentication
                .anyRequest().authenticated()
            )
            // Custom login page
            .formLogin(form -> form
                .loginPage("/login")
                .successHandler(customSuccessHandler())
            )
            // Logout configuration
            .logout(logout -> logout
                .logoutSuccessUrl("/login?logout")
            );
        
        return http.build();
    }
}
```

---

### 3. `controllers/` - HTTP Request Handlers

Controllers receive HTTP requests and return responses (HTML views or JSON).

#### Public Controllers

| File | URLs | Purpose |
|------|------|---------|
| `HomeController.java` | `/` | Store home page |
| `StoreController.java` | `/shop/**` | Product listing, detail |
| `AuthController.java` | `/login`, `/register` | Authentication |
| `CheckoutController.java` | `/checkout/**` | Checkout process |
| `CustomerOrderController.java` | `/orders/**` | Customer order history |

#### Admin Controllers (`admin/`)

| File | URLs | Purpose |
|------|------|---------|
| `AdminController.java` | `/admin` | Dashboard |
| `CategoryController.java` | `/admin/categories/**` | Category CRUD |
| `ProductController.java` | `/admin/products/**` | Product CRUD |
| `ProductVariantAdminController.java` | `/admin/products/{id}/variants/**` | Variant CRUD |
| `OrderController.java` | `/admin/orders/**` | Order management |
| `UserController.java` | `/admin/users/**` | User management |
| `AdminActivityLogController.java` | `/admin/activity` | Activity log |
| `CustomErrorController.java` | `/error` | Error page handling |

#### Controller Example

```java
@Controller
@RequestMapping("/admin/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;
    private final AdminActivityLogService activityLogService;

    // LIST: GET /admin/categories
    @GetMapping
    public String list(Model model) {
        model.addAttribute("categories", categoryService.findAll());
        return "admin/categories/list";  // → templates/admin/categories/list.html
    }

    // NEW FORM: GET /admin/categories/new
    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("categoryDto", new CategoryDto());
        model.addAttribute("parentCategories", getParentCategories());
        return "admin/categories/new";
    }

    // CREATE: POST /admin/categories/new
    @PostMapping("/new")
    public String create(@Valid @ModelAttribute CategoryDto dto,
                         BindingResult result,
                         RedirectAttributes flash) {
        if (result.hasErrors()) {
            return "admin/categories/new";
        }
        
        CategoryDto created = categoryService.add(dto);
        activityLogService.log("CREATE", "Category", created.getId(), created.getName(), null);
        flash.addFlashAttribute("success", "Category created!");
        
        return "redirect:/admin/categories";
    }
}
```

---

### 4. `entities/` - Database Tables

Entities are Java classes that map to database tables using JPA annotations.

#### Core Entities

| File | Table | Purpose |
|------|-------|---------|
| `UserEntity.java` | `users` | User accounts |
| `OrderEntity.java` | `orders` | Customer orders |
| `OrderItemEntity.java` | `order_items` | Items in orders |
| `AdminActivityLogEntity.java` | `admin_activity_logs` | Audit trail |

#### Catalog Entities (`catalog/`)

| File | Table | Purpose |
|------|-------|---------|
| `CategoryEntity.java` | `categories` | Product categories |
| `ProductEntity.java` | `products` | Products |
| `ProductVariantEntity.java` | `product_variants` | Size/color variants |

#### Entity Example

```java
@Entity
@Table(name = "products")
@Getter @Setter
@NoArgsConstructor
public class ProductEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String slug;  // URL-friendly name

    @Column(columnDefinition = "TEXT")
    private String description;

    private boolean active = true;

    // Many products belong to one category
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private CategoryEntity category;

    // One product has many variants
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL)
    private List<ProductVariantEntity> variants = new ArrayList<>();

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
```

---

### 5. `dtos/` - Data Transfer Objects

DTOs are simple objects for transferring data between layers. They:
- Hide entity internals from views
- Validate user input
- Shape data for specific use cases

#### Why Use DTOs?

```java
// ❌ BAD: Exposing entity directly
@GetMapping("/users/{id}")
public UserEntity getUser(@PathVariable Long id) {
    return userRepository.findById(id);
    // Exposes password hash, internal IDs, etc.!
}

// ✅ GOOD: Using DTO
@GetMapping("/users/{id}")
public UserResponseDto getUser(@PathVariable Long id) {
    UserEntity user = userRepository.findById(id);
    return new UserResponseDto(user.getId(), user.getUsername(), user.getEmail());
    // Only exposes safe, needed fields
}
```

#### DTO Structure

```
dtos/
├── CategoryDto.java           # Category create/update
├── ProductDto.java            # Product create/update
├── ProductVariantDto.java     # Variant create/update
├── CheckoutDto.java           # Checkout form data
├── PaymentIntentDto.java      # Stripe payment info
├── errors/
│   └── ApiErrorDto.java       # Error responses
├── order/
│   ├── OrderCreateDto.java    # Create order request
│   ├── OrderResponseDto.java  # Order details response
│   ├── OrderSummaryDto.java   # Order list item
│   └── OrderItemCreateDto.java
└── user/
    ├── UserRegistrationDto.java  # Registration form
    ├── UserResponseDto.java      # User details
    └── UserSummaryDto.java       # User list item
```

---

### 6. `enums/` - Enumeration Types

```java
// Role.java - User roles
public enum Role {
    CUSTOMER,  // Regular user
    ADMIN      // Administrator
}

// OrderStatus.java - Order lifecycle
public enum OrderStatus {
    PENDING,     // Just created
    CREATED,     // Confirmed
    PAID,        // Payment received
    SHIPPED,     // Sent to customer
    DELIVERED,   // Received by customer
    CANCELLED    // Cancelled
}

// Size.java - Product sizes
public enum Size {
    XS, S, M, L, XL, XXL
}
```

---

### 7. `infrastructure/services/` - Business Logic

Services contain the business logic. They:
- Orchestrate multiple repositories
- Validate business rules
- Handle transactions
- Keep controllers thin

#### Service Structure

```
infrastructure/
├── genericservices/
│   └── GenericService.java     # Base CRUD interface
└── services/
    ├── CategoryService.java
    ├── ProductService.java
    ├── ProductVariantService.java
    ├── OrderService.java
    ├── UserService.java
    ├── PaymentService.java
    ├── InventoryService.java
    ├── FileStorageService.java
    ├── AdminActivityLogService.java
    └── impl/                    # Implementations
        ├── CategoryServiceImpl.java
        ├── ProductServiceImpl.java
        └── ...
```

#### Service Example

```java
public interface CategoryService extends GenericService<CategoryDto, Long> {
    List<CategoryDto> findRootCategories();
    List<CategoryDto> findByParentId(Long parentId);
}

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository repository;
    private final CategoryMapper mapper;

    @Override
    public List<CategoryDto> findAll() {
        return mapper.toDtoList(repository.findAll());
    }

    @Override
    public CategoryDto add(CategoryDto dto) {
        // Validate
        if (repository.existsBySlug(dto.getSlug())) {
            throw new DuplicateResourceException("Slug already exists");
        }
        
        // Convert DTO → Entity
        CategoryEntity entity = mapper.toEntity(dto);
        
        // Save
        CategoryEntity saved = repository.save(entity);
        
        // Convert Entity → DTO and return
        return mapper.toDto(saved);
    }
}
```

---

### 8. `mappers/` - Entity ↔ DTO Conversion

MapStruct generates implementation at compile time:

```java
@Mapper(componentModel = "spring")
public interface CategoryMapper {
    
    CategoryDto toDto(CategoryEntity entity);
    
    CategoryEntity toEntity(CategoryDto dto);
    
    List<CategoryDto> toDtoList(List<CategoryEntity> entities);
    
    // Custom mapping
    @Mapping(target = "parentId", source = "parent.id")
    @Mapping(target = "parentName", source = "parent.name")
    CategoryDto toDtoWithParent(CategoryEntity entity);
}
```

Generated code (in `target/generated-sources/`):

```java
@Component
public class CategoryMapperImpl implements CategoryMapper {
    
    @Override
    public CategoryDto toDto(CategoryEntity entity) {
        if (entity == null) return null;
        
        CategoryDto dto = new CategoryDto();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setSlug(entity.getSlug());
        // ... more fields
        return dto;
    }
}
```

---

### 9. `repositories/` - Database Access

Spring Data JPA repositories provide CRUD operations automatically:

```java
public interface ProductRepository extends JpaRepository<ProductEntity, Long> {
    
    // Auto-generated: findById, findAll, save, delete, etc.
    
    // Custom query methods (Spring generates SQL from method name)
    Optional<ProductEntity> findBySlug(String slug);
    
    List<ProductEntity> findByCategoryId(Long categoryId);
    
    boolean existsBySlug(String slug);
    
    // Custom JPQL query
    @Query("SELECT p FROM products p WHERE p.active = true ORDER BY p.createdAt DESC")
    List<ProductEntity> findActiveProductsOrderByNewest();
}
```

---

## Resources Structure

```
src/main/resources/
├── application.properties      # Main configuration
├── static/                     # Static files (served directly)
│   ├── favicon.ico
│   └── assets/
│       ├── admin/             # Admin panel assets
│       │   ├── css/admin.css
│       │   └── js/admin.js
│       └── public/            # Store assets
│           ├── css/public.css
│           └── js/public.js
└── templates/                  # Thymeleaf templates
    ├── admin/                  # Admin panel views
    │   ├── shared/_layout.html # Admin layout
    │   ├── dashboard.html
    │   ├── categories/
    │   ├── products/
    │   ├── variants/
    │   ├── orders/
    │   └── users/
    ├── store/                  # Customer store views
    │   ├── fragments/_layout.html
    │   ├── home.html
    │   ├── shop.html
    │   ├── product.html
    │   ├── cart.html
    │   └── checkout.html
    ├── auth/                   # Authentication views
    │   ├── login.html
    │   ├── register.html
    │   └── profile.html
    ├── error/                  # Error pages
    │   ├── 400.html
    │   ├── 403.html
    │   ├── 404.html
    │   └── 500.html
    └── fragments/              # Shared fragments
        └── layout.html
```

---

## Key Configuration Files

### pom.xml

Maven configuration defining:
- Project metadata
- Java version (21)
- Dependencies
- Build plugins (Lombok, MapStruct processors)

### compose.yaml

Docker Compose file for local PostgreSQL:

```yaml
services:
  postgres:
    image: 'postgres:latest'
    environment:
      - POSTGRES_DB=auvier_db
      - POSTGRES_USER=auvier_user
      - POSTGRES_PASSWORD=auvier_pass
    ports:
      - '5432:5432'
```

---

## Next Steps

- Continue to [04-DATABASE-DESIGN.md](./04-DATABASE-DESIGN.md) to understand the data model
- Or read [05-ARCHITECTURE-PATTERNS.md](./05-ARCHITECTURE-PATTERNS.md) for design patterns
