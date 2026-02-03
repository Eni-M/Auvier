# 12 - Error Handling

This document explains how errors are handled throughout the Auvier application.

---

## Overview

Error handling in Auvier covers:
- Custom exceptions for business logic
- Global exception handling
- User-friendly error pages
- Logging for debugging

---

## Custom Exceptions

### Exception Hierarchy

```
RuntimeException
└── AuvierException (Base)
    ├── ResourceNotFoundException (404)
    ├── DuplicateResourceException (400)
    ├── InsufficientStockException (400)
    └── PaymentException (400)
```

### Implementation

```java
// Base exception
public class AuvierException extends RuntimeException {
    public AuvierException(String message) {
        super(message);
    }

    public AuvierException(String message, Throwable cause) {
        super(message, cause);
    }
}

// Resource not found (404)
public class ResourceNotFoundException extends AuvierException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}

// Duplicate resource (e.g., username exists)
public class DuplicateResourceException extends AuvierException {
    public DuplicateResourceException(String message) {
        super(message);
    }
}

// Insufficient stock
public class InsufficientStockException extends AuvierException {
    public InsufficientStockException(String message) {
        super(message);
    }
}
```

### Usage in Services

```java
@Service
public class ProductServiceImpl implements ProductService {

    @Override
    public ProductDto findOne(Long id) {
        return productRepository.findById(id)
            .map(mapper::toDto)
            .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));
    }

    @Override
    public ProductDto add(ProductDto dto) {
        if (productRepository.existsBySlug(dto.getSlug())) {
            throw new DuplicateResourceException("Product slug already exists: " + dto.getSlug());
        }
        // ... create product
    }
}

@Service
public class InventoryServiceImpl implements InventoryService {

    @Override
    public void validateStock(Long variantId, int quantity) {
        ProductVariantEntity variant = getVariant(variantId);
        if (variant.getStock() < quantity) {
            throw new InsufficientStockException(
                "Not enough stock for " + variant.getSku() +
                ". Requested: " + quantity + ", Available: " + variant.getStock()
            );
        }
    }
}
```

---

## Global Exception Handler

### For Web Pages

```java
@ControllerAdvice
public class WebExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(WebExceptionHandler.class);

    // Handle resource not found
    @ExceptionHandler(ResourceNotFoundException.class)
    public String handleNotFound(ResourceNotFoundException ex, Model model) {
        log.warn("Resource not found: {}", ex.getMessage());
        model.addAttribute("message", ex.getMessage());
        return "error/404";
    }

    // Handle duplicate resource (e.g., username exists)
    @ExceptionHandler(DuplicateResourceException.class)
    public String handleDuplicate(DuplicateResourceException ex, RedirectAttributes flash) {
        log.warn("Duplicate resource: {}", ex.getMessage());
        flash.addFlashAttribute("error", ex.getMessage());
        return "redirect:/admin";
    }

    // Handle validation errors
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public String handleValidation(MethodArgumentNotValidException ex, Model model) {
        log.warn("Validation error: {}", ex.getMessage());
        model.addAttribute("errors", ex.getBindingResult().getAllErrors());
        return "error/400";
    }

    // Handle access denied
    @ExceptionHandler(AccessDeniedException.class)
    public String handleAccessDenied(AccessDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage());
        return "error/403";
    }

    // Catch-all for unexpected errors
    @ExceptionHandler(Exception.class)
    public String handleGeneral(Exception ex, Model model) {
        String traceId = UUID.randomUUID().toString().substring(0, 8);
        log.error("TraceId: {} | Unexpected error: ", traceId, ex);
        model.addAttribute("traceId", traceId);
        return "error/500";
    }
}
```

### For REST APIs

```java
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiErrorDto handleNotFound(ResourceNotFoundException ex) {
        return new ApiErrorDto(
            HttpStatus.NOT_FOUND.value(),
            "Not Found",
            ex.getMessage()
        );
    }

    @ExceptionHandler(DuplicateResourceException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiErrorDto handleDuplicate(DuplicateResourceException ex) {
        return new ApiErrorDto(
            HttpStatus.BAD_REQUEST.value(),
            "Bad Request",
            ex.getMessage()
        );
    }
}
```

---

## Custom Error Controller

Spring Boot's default error page is replaced with a custom controller:

```java
@Controller
@Slf4j
public class CustomErrorController implements ErrorController {

    @RequestMapping("/error")
    public String handleError(HttpServletRequest request, Model model) {
        // Get error details from request
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        Object message = request.getAttribute(RequestDispatcher.ERROR_MESSAGE);
        Object exception = request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);

        // Generate trace ID for support
        String traceId = generateTraceId();

        int statusCode = 500;
        if (status != null) {
            statusCode = Integer.parseInt(status.toString());
        }

        String errorMessage = message != null ? message.toString() : "An error occurred";

        // Log the error
        if (statusCode >= 500) {
            log.error("TraceId: {} | Error {} at {}: {}",
                traceId, statusCode,
                request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI),
                exception != null ? exception : errorMessage);
        } else {
            log.warn("TraceId: {} | Error {} at {}: {}",
                traceId, statusCode,
                request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI),
                errorMessage);
        }

        // Add attributes to model
        model.addAttribute("status", statusCode);
        model.addAttribute("error", HttpStatus.valueOf(statusCode).getReasonPhrase());
        model.addAttribute("message", errorMessage);
        model.addAttribute("traceId", traceId);

        // Return appropriate error page
        return switch (statusCode) {
            case 400 -> "error/400";
            case 403 -> "error/403";
            case 404 -> "error/404";
            case 500 -> "error/500";
            default -> statusCode >= 400 && statusCode < 500 ? "error/400" : "error/500";
        };
    }

    private String generateTraceId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
```

---

## Error Pages

### Structure

```
templates/error/
├── 400.html    # Bad Request
├── 403.html    # Forbidden
├── 404.html    # Not Found
└── 500.html    # Server Error
```

### 500.html Example

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <title>Server Error | AUVIER</title>
    <style>
        /* Inline styles for standalone error page */
        body {
            font-family: 'Montserrat', sans-serif;
            background: #ffffff;
            min-height: 100vh;
            display: flex;
            flex-direction: column;
        }
        .error-container {
            flex: 1;
            display: flex;
            align-items: center;
            justify-content: center;
            text-align: center;
        }
        .error-status {
            font-size: 6rem;
            font-weight: 300;
        }
        .error-title {
            font-size: 1.75rem;
            margin: 1rem 0;
        }
        .error-message {
            color: #666;
            margin-bottom: 1.5rem;
        }
        .error-trace {
            background: #f5f5f5;
            padding: 0.75rem 1rem;
            border-radius: 6px;
            font-size: 0.8125rem;
            margin-bottom: 2rem;
        }
        .error-trace-id {
            font-family: monospace;
            font-weight: 500;
        }
    </style>
</head>
<body>
    <header class="error-header">
        <a th:href="@{/}" class="error-logo">Auvier</a>
    </header>

    <main class="error-container">
        <div>
            <div class="error-status">500</div>
            <h1 class="error-title">Something Went Wrong</h1>
            <p class="error-message">
                We're experiencing technical difficulties. Our team has been notified.
            </p>

            <!-- Trace ID for support -->
            <div class="error-trace" th:if="${traceId != null}">
                <span>Reference ID:</span>
                <span class="error-trace-id" th:text="${traceId}">abc12345</span>
            </div>

            <div class="error-actions">
                <a th:href="@{/}" class="error-btn">Back to Home</a>
                <button onclick="location.reload()">Try Again</button>
            </div>
        </div>
    </main>

    <footer class="error-footer">
        &copy; 2026 Auvier. All rights reserved.
    </footer>
</body>
</html>
```

---

## Logging

### Configuration (application.properties)

```properties
# Logging levels
logging.level.root=INFO
logging.level.com.auvier=DEBUG

# Log file
logging.file.name=logs/application.log

# Console pattern
logging.pattern.console=%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n

# File pattern
logging.pattern.file=%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n

# Rolling policy
logging.logback.rollingpolicy.max-file-size=10MB
logging.logback.rollingpolicy.max-history=30
```

### Using Loggers

```java
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j  // Lombok creates: private static final Logger log = LoggerFactory.getLogger(...)
public class OrderServiceImpl implements OrderService {

    @Override
    public OrderResponseDto createOrder(UserEntity user, OrderCreateDto dto) {
        log.info("Creating order for user: {}", user.getUsername());

        try {
            // ... order logic

            log.info("Order created successfully. ID: {}, Total: {}", order.getId(), total);
            return orderMapper.toResponseDto(order);

        } catch (Exception e) {
            log.error("Failed to create order for user {}: {}", user.getUsername(), e.getMessage(), e);
            throw e;
        }
    }
}
```

### Log Levels

| Level | Use Case | Example |
|-------|----------|---------|
| `TRACE` | Very detailed debugging | Method entry/exit |
| `DEBUG` | Debugging information | Variable values |
| `INFO` | Normal operations | "Order created" |
| `WARN` | Potential issues | "Stock low" |
| `ERROR` | Errors that need attention | "Payment failed" |

---

## Form Validation Errors

### DTO Validation

```java
public class ProductDto {

    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name must be less than 100 characters")
    private String name;

    @NotBlank(message = "Slug is required")
    @Pattern(regexp = "^[a-z0-9-]+$", message = "Slug must be lowercase with hyphens")
    private String slug;

    @NotNull(message = "Category is required")
    private Long categoryId;
}
```

### Controller Handling

```java
@PostMapping("/new")
public String create(@Valid @ModelAttribute("productDto") ProductDto dto,
                     BindingResult result,
                     Model model) {
    // Check for validation errors
    if (result.hasErrors()) {
        // Re-add data needed for form
        model.addAttribute("categories", categoryService.findAll());
        return "admin/products/new";  // Return to form with errors
    }

    // Process valid form
    productService.add(dto);
    return "redirect:/admin/products";
}
```

### Displaying Errors in Template

```html
<form th:object="${productDto}">
    <!-- Global errors -->
    <div th:if="${#fields.hasGlobalErrors()}" class="v-alert v-alert--error">
        <p th:each="err : ${#fields.globalErrors()}" th:text="${err}">Error</p>
    </div>

    <!-- Field with error -->
    <div class="v-form__group">
        <label class="v-label">Name</label>
        <input type="text" th:field="*{name}"
               th:classappend="${#fields.hasErrors('name')} ? 'v-input--error' : ''"
               class="v-input">
        <span th:if="${#fields.hasErrors('name')}"
              th:errors="*{name}"
              class="v-error">Error message here</span>
    </div>

    <!-- Field without error -->
    <div class="v-form__group">
        <label class="v-label">Description</label>
        <textarea th:field="*{description}" class="v-textarea"></textarea>
    </div>
</form>
```

---

## Flash Messages

### Setting Flash Messages

```java
@PostMapping("/new")
public String create(..., RedirectAttributes flash) {
    try {
        productService.add(dto);
        flash.addFlashAttribute("success", "Product created successfully!");
        return "redirect:/admin/products";
    } catch (DuplicateResourceException e) {
        flash.addFlashAttribute("error", e.getMessage());
        return "redirect:/admin/products/new";
    }
}
```

### Displaying Flash Messages

```html
<!-- In layout or page -->
<div th:if="${success}" class="v-alert v-alert--success">
    <i class="bi bi-check-circle"></i>
    <span th:text="${success}">Success message</span>
</div>

<div th:if="${error}" class="v-alert v-alert--error">
    <i class="bi bi-exclamation-circle"></i>
    <span th:text="${error}">Error message</span>
</div>
```

---

## Best Practices

1. **Use Custom Exceptions**: Don't throw generic `RuntimeException`
2. **Log at Right Level**: INFO for normal ops, ERROR for failures
3. **Include Context**: Log user ID, order ID, etc.
4. **Hide Implementation Details**: Show user-friendly messages
5. **Provide Trace IDs**: Help users report issues
6. **Validate Early**: Catch errors in controller/service, not database

---

## Next Steps

- Read [13-TESTING-GUIDE.md](./13-TESTING-GUIDE.md) for testing
- Or [14-DEPLOYMENT-GUIDE.md](./14-DEPLOYMENT-GUIDE.md) for deployment
