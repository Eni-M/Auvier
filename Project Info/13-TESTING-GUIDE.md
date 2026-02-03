# 13 - Testing Guide

This document explains how to test the Auvier application.

---

## Test Structure

```
src/test/java/com/auvier/
├── AuvierApplicationTests.java     # Context loads test
├── controllers/                    # Controller tests
├── services/                       # Service tests
└── repositories/                   # Repository tests
```

---

## Types of Tests

| Type | What to Test | Speed |
|------|--------------|-------|
| Unit Tests | Services, Mappers | Fast |
| Integration Tests | Repositories, Controllers | Medium |
| End-to-End Tests | Full flows | Slow |

---

## Unit Testing Services

### Example: CategoryServiceTest

```java
@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private CategoryMapper categoryMapper;

    @InjectMocks
    private CategoryServiceImpl categoryService;

    @Test
    void findAll_ShouldReturnAllCategories() {
        // Given
        List<CategoryEntity> entities = List.of(
            createCategory(1L, "Shirts"),
            createCategory(2L, "Pants")
        );
        List<CategoryDto> dtos = List.of(
            createCategoryDto(1L, "Shirts"),
            createCategoryDto(2L, "Pants")
        );

        when(categoryRepository.findAll()).thenReturn(entities);
        when(categoryMapper.toDtoList(entities)).thenReturn(dtos);

        // When
        List<CategoryDto> result = categoryService.findAll();

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getName()).isEqualTo("Shirts");
        verify(categoryRepository).findAll();
    }

    @Test
    void findOne_WhenNotFound_ShouldThrowException() {
        // Given
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(ResourceNotFoundException.class, () -> {
            categoryService.findOne(99L);
        });
    }

    @Test
    void add_WhenSlugExists_ShouldThrowDuplicateException() {
        // Given
        CategoryDto dto = new CategoryDto();
        dto.setName("Shirts");
        dto.setSlug("shirts");

        when(categoryRepository.existsBySlug("shirts")).thenReturn(true);

        // When/Then
        assertThrows(DuplicateResourceException.class, () -> {
            categoryService.add(dto);
        });
    }

    // Helper methods
    private CategoryEntity createCategory(Long id, String name) {
        CategoryEntity entity = new CategoryEntity();
        entity.setId(id);
        entity.setName(name);
        entity.setSlug(name.toLowerCase());
        return entity;
    }

    private CategoryDto createCategoryDto(Long id, String name) {
        CategoryDto dto = new CategoryDto();
        dto.setId(id);
        dto.setName(name);
        dto.setSlug(name.toLowerCase());
        return dto;
    }
}
```

### Example: OrderServiceTest

```java
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private InventoryService inventoryService;

    @Mock
    private OrderMapper orderMapper;

    @InjectMocks
    private OrderServiceImpl orderService;

    @Test
    void createOrder_ShouldCalculateTotalCorrectly() {
        // Given
        UserEntity user = new UserEntity();
        user.setId(1L);
        user.setUsername("testuser");

        OrderCreateDto orderDto = new OrderCreateDto();
        orderDto.setShippingAddress("123 Test St");
        orderDto.setItems(List.of(
            new OrderItemCreateDto(1L, 2),  // 2 items of variant 1
            new OrderItemCreateDto(2L, 1)   // 1 item of variant 2
        ));

        ProductVariantEntity variant1 = createVariant(1L, new BigDecimal("50.00"));
        ProductVariantEntity variant2 = createVariant(2L, new BigDecimal("30.00"));

        when(inventoryService.getVariant(1L)).thenReturn(variant1);
        when(inventoryService.getVariant(2L)).thenReturn(variant2);
        when(orderRepository.save(any())).thenAnswer(i -> {
            OrderEntity order = i.getArgument(0);
            order.setId(1L);
            return order;
        });

        // When
        OrderResponseDto result = orderService.createOrder(user, orderDto);

        // Then
        verify(inventoryService).validateStock(1L, 2);
        verify(inventoryService).validateStock(2L, 1);
        verify(inventoryService).reserveStock(1L, 2);
        verify(inventoryService).reserveStock(2L, 1);

        ArgumentCaptor<OrderEntity> orderCaptor = ArgumentCaptor.forClass(OrderEntity.class);
        verify(orderRepository, times(2)).save(orderCaptor.capture());

        OrderEntity savedOrder = orderCaptor.getValue();
        // Total: (50 * 2) + (30 * 1) = 130
        assertThat(savedOrder.getTotalAmount()).isEqualByComparingTo(new BigDecimal("130.00"));
    }

    @Test
    void createOrder_WhenInsufficientStock_ShouldThrowException() {
        // Given
        UserEntity user = new UserEntity();
        OrderCreateDto orderDto = new OrderCreateDto();
        orderDto.setItems(List.of(new OrderItemCreateDto(1L, 100)));

        doThrow(new InsufficientStockException("Not enough stock"))
            .when(inventoryService).validateStock(1L, 100);

        // When/Then
        assertThrows(InsufficientStockException.class, () -> {
            orderService.createOrder(user, orderDto);
        });
    }
}
```

---

## Integration Testing

### Repository Tests

```java
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ProductRepositoryTest {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Test
    void findBySlug_ShouldReturnProduct() {
        // Given
        CategoryEntity category = new CategoryEntity();
        category.setName("Shirts");
        category.setSlug("shirts");
        category = categoryRepository.save(category);

        ProductEntity product = new ProductEntity();
        product.setName("Blue Shirt");
        product.setSlug("blue-shirt");
        product.setCategory(category);
        productRepository.save(product);

        // When
        Optional<ProductEntity> result = productRepository.findBySlug("blue-shirt");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Blue Shirt");
    }

    @Test
    void findByCategoryId_ShouldReturnMatchingProducts() {
        // Given
        CategoryEntity category = createAndSaveCategory("Pants", "pants");

        createAndSaveProduct("Jeans", "jeans", category);
        createAndSaveProduct("Chinos", "chinos", category);

        // When
        List<ProductEntity> result = productRepository.findByCategoryId(category.getId());

        // Then
        assertThat(result).hasSize(2);
    }
}
```

### Controller Tests

```java
@WebMvcTest(CategoryController.class)
@Import(SecurityConfig.class)
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CategoryService categoryService;

    @MockBean
    private AdminActivityLogService activityLogService;

    @Test
    @WithMockUser(roles = "ADMIN")
    void list_ShouldReturnCategoryList() throws Exception {
        // Given
        when(categoryService.findAll()).thenReturn(List.of(
            createCategoryDto(1L, "Shirts"),
            createCategoryDto(2L, "Pants")
        ));

        // When/Then
        mockMvc.perform(get("/admin/categories"))
            .andExpect(status().isOk())
            .andExpect(view().name("admin/categories/list"))
            .andExpect(model().attributeExists("categories"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_WithValidData_ShouldRedirect() throws Exception {
        // Given
        CategoryDto created = createCategoryDto(1L, "Shirts");
        when(categoryService.add(any())).thenReturn(created);

        // When/Then
        mockMvc.perform(post("/admin/categories/new")
                .param("name", "Shirts")
                .param("slug", "shirts")
                .with(csrf()))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/admin/categories"));

        verify(activityLogService).log(eq("CREATE"), eq("Category"), eq(1L), eq("Shirts"), any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_WithInvalidData_ShouldReturnForm() throws Exception {
        // When/Then
        mockMvc.perform(post("/admin/categories/new")
                .param("name", "")  // Invalid: empty name
                .param("slug", "")  // Invalid: empty slug
                .with(csrf()))
            .andExpect(status().isOk())
            .andExpect(view().name("admin/categories/new"))
            .andExpect(model().hasErrors());
    }

    @Test
    void list_WithoutAuthentication_ShouldRedirectToLogin() throws Exception {
        mockMvc.perform(get("/admin/categories"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void list_WithWrongRole_ShouldReturn403() throws Exception {
        mockMvc.perform(get("/admin/categories"))
            .andExpect(status().isForbidden());
    }
}
```

---

## Test Utilities

### Test Data Builder

```java
public class TestDataBuilder {

    public static UserEntity.UserEntityBuilder user() {
        return UserEntity.builder()
            .username("testuser")
            .email("test@example.com")
            .password("encoded_password")
            .role(Role.CUSTOMER);
    }

    public static ProductEntity.ProductEntityBuilder product() {
        return ProductEntity.builder()
            .name("Test Product")
            .slug("test-product")
            .active(true);
    }

    public static ProductVariantEntity.ProductVariantEntityBuilder variant() {
        return ProductVariantEntity.builder()
            .sku("TEST-001")
            .price(new BigDecimal("99.99"))
            .stock(100)
            .color("Blue")
            .size(Size.M)
            .active(true);
    }
}

// Usage
UserEntity user = TestDataBuilder.user()
    .username("john")
    .role(Role.ADMIN)
    .build();
```

### Custom Assertions

```java
public class OrderAssertions {

    public static void assertOrderHasStatus(OrderEntity order, OrderStatus expected) {
        assertThat(order.getStatus())
            .as("Order %d should have status %s", order.getId(), expected)
            .isEqualTo(expected);
    }

    public static void assertOrderTotalEquals(OrderEntity order, BigDecimal expected) {
        assertThat(order.getTotalAmount())
            .as("Order total should be %s", expected)
            .isEqualByComparingTo(expected);
    }
}
```

---

## Running Tests

### Command Line

```bash
# Run all tests
./mvnw test

# Run specific test class
./mvnw test -Dtest=CategoryServiceTest

# Run specific test method
./mvnw test -Dtest=CategoryServiceTest#findAll_ShouldReturnAllCategories

# Run with coverage report
./mvnw test jacoco:report
```

### IDE (IntelliJ)

1. Right-click on test class → Run
2. Or click green play button next to test method

---

## Test Configuration

### Test application.properties

```properties
# src/test/resources/application.properties

# Use H2 in-memory database for tests
spring.datasource.url=jdbc:h2:mem:testdb
spring.datasource.driver-class-name=org.h2.Driver
spring.jpa.hibernate.ddl-auto=create-drop

# Disable Flyway for tests
spring.flyway.enabled=false

# Faster tests
spring.jpa.show-sql=false
logging.level.root=WARN
```

---

## What to Test

### Must Test
- Service business logic
- Validation rules
- Security access controls
- Error handling

### Good to Test
- Repository custom queries
- Controller request/response
- Mapper correctness

### Usually Skip
- Getters/setters
- Configuration classes
- Simple pass-through methods

---

## Test Coverage Goals

| Layer | Target |
|-------|--------|
| Services | 80%+ |
| Controllers | 70%+ |
| Repositories | 60%+ |
| Overall | 70%+ |

---

## Next Steps

- Read [14-DEPLOYMENT-GUIDE.md](./14-DEPLOYMENT-GUIDE.md) for deployment
- Or [15-FUTURE-IMPROVEMENTS.md](./15-FUTURE-IMPROVEMENTS.md) for roadmap
