# 07 - Admin Panel

This document explains the admin panel features, UI design, and implementation details.

---

## Overview

The admin panel is a dark-themed, futuristic dashboard for managing the e-commerce store. It's accessible only to users with the `ADMIN` role.

**URL**: `http://localhost:2525/admin`

---

## Features

| Feature | URL | Description |
|---------|-----|-------------|
| Dashboard | `/admin` | Overview with statistics |
| Categories | `/admin/categories` | Hierarchical category management |
| Products | `/admin/products` | Product CRUD |
| Variants | `/admin/products/{id}/variants` | Size/color variants |
| Orders | `/admin/orders` | Order management |
| Users | `/admin/users` | User management |
| Activity Log | `/admin/activity` | Audit trail |

---

## Design System

### Color Palette

```css
:root {
    /* Backgrounds */
    --admin-bg: #0d0d0f;              /* Page background */
    --admin-surface: #141418;          /* Card background */
    --admin-surface-hover: #1a1a1f;    /* Hover state */

    /* Borders */
    --admin-border: rgba(255, 255, 255, 0.08);
    --admin-border-hover: rgba(255, 255, 255, 0.15);

    /* Text */
    --admin-text: rgba(255, 255, 255, 0.92);
    --admin-text-muted: rgba(255, 255, 255, 0.5);

    /* Accent Colors */
    --admin-primary: #3b82f6;          /* Blue - primary actions */
    --admin-success: #10b981;          /* Green - success states */
    --admin-warning: #f59e0b;          /* Orange - warnings */
    --admin-danger: #ef4444;           /* Red - destructive actions */
}
```

### Typography

- **Headings**: System font stack (San Francisco, Segoe UI)
- **Body**: Same, with 14px base size
- **Monospace**: For IDs, SKUs, code

### Components

1. **Cards** (`.v-card`): Content containers with subtle border
2. **Tables** (`.v-table`): Data display with hover states
3. **Buttons** (`.v-btn`): Various styles (primary, ghost, danger)
4. **Badges** (`.v-badge`): Status indicators
5. **Modals** (`.v-modal`): Confirmation dialogs

---

## Layout Structure

### _layout.html (Admin Layout)

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org"
      xmlns:sec="http://www.thymeleaf.org/extras/spring-security">
<head>
    <title th:text="${title} + ' | Auvier Admin'">Admin</title>
    <link rel="stylesheet" th:href="@{/assets/admin/css/admin.css}">
</head>
<body class="v-admin-body">
    <!-- Top Navigation Bar -->
    <header class="v-topbar">
        <div class="v-topbar__inner">
            <!-- Logo (left) -->
            <a href="/admin" class="v-brand">AUVIER</a>

            <!-- Navigation (center) -->
            <nav class="v-topnav">
                <a href="/admin" class="v-topnav__link">Dashboard</a>
                <a href="/admin/orders" class="v-topnav__link">Orders</a>
                <a href="/admin/categories" class="v-topnav__link">Categories</a>
                <a href="/admin/products" class="v-topnav__link">Products</a>
                <a href="/admin/users" class="v-topnav__link">Users</a>
            </nav>

            <!-- Profile Dropdown (right) -->
            <div class="v-admin-profile">
                <!-- ... profile dropdown content ... -->
            </div>
        </div>
    </header>

    <!-- Main Content -->
    <main class="v-main">
        <th:block th:replace="${content}"/>
    </main>

    <script th:src="@{/assets/admin/js/admin.js}"></script>
</body>
</html>
```

---

## CRUD Pattern

Each admin feature follows the same CRUD pattern:

### Controller Structure

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
        return "admin/categories/list";
    }

    // VIEW: GET /admin/categories/{id}/view
    @GetMapping("/{id}/view")
    public String view(@PathVariable Long id, Model model) {
        model.addAttribute("category", categoryService.findOne(id));
        return "admin/categories/view";
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
                         Model model,
                         RedirectAttributes flash) {
        if (result.hasErrors()) {
            model.addAttribute("parentCategories", getParentCategories());
            return "admin/categories/new";
        }

        CategoryDto created = categoryService.add(dto);
        activityLogService.log("CREATE", "Category", created.getId(), created.getName(), null);
        flash.addFlashAttribute("success", "Category created successfully!");
        return "redirect:/admin/categories";
    }

    // EDIT FORM: GET /admin/categories/{id}/edit
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("categoryDto", categoryService.findOne(id));
        model.addAttribute("parentCategories", getParentCategories());
        return "admin/categories/edit";
    }

    // UPDATE: POST /admin/categories/{id}/edit
    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute CategoryDto dto,
                         BindingResult result,
                         Model model,
                         RedirectAttributes flash) {
        if (result.hasErrors()) {
            model.addAttribute("parentCategories", getParentCategories());
            return "admin/categories/edit";
        }

        categoryService.update(id, dto);
        activityLogService.log("UPDATE", "Category", id, dto.getName(), null);
        flash.addFlashAttribute("success", "Category updated!");
        return "redirect:/admin/categories";
    }

    // DELETE: POST /admin/categories/{id}/delete
    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes flash) {
        CategoryDto category = categoryService.findOne(id);
        categoryService.delete(id);
        activityLogService.log("DELETE", "Category", id, category.getName(), null);
        flash.addFlashAttribute("success", "Category deleted!");
        return "redirect:/admin/categories";
    }
}
```

### Template Structure

```
templates/admin/categories/
├── list.html      # Table of all categories
├── view.html      # Single category details
├── new.html       # Create form
├── edit.html      # Edit form
└── _newOrEdit.html # Shared form fields (included in new.html and edit.html)
```

---

## Category Management

### Hierarchical Categories

Categories support parent-child relationships:

```
Parent Category (parent_id = null)
├── Child Category 1 (parent_id = parent.id)
└── Child Category 2 (parent_id = parent.id)
```

### Parent Category Dropdown

```html
<!-- In _newOrEdit.html -->
<div class="v-form__group">
    <label class="v-label">Parent Category (Optional)</label>
    <select th:field="*{parentId}" class="v-select">
        <option value="">None (Top Level)</option>
        <option th:each="parent : ${parentCategories}"
                th:value="${parent.id}"
                th:text="${parent.name}">Parent</option>
    </select>
</div>
```

### Controller Logic

```java
private List<CategoryDto> getParentCategories() {
    return categoryService.findAll().stream()
        .filter(c -> c.getParentId() == null)  // Only root categories
        .toList();
}
```

---

## Product Management

### Products with Variants

```
Product (name, description, category)
├── Variant 1 (SKU, price, stock, size, color)
├── Variant 2 (SKU, price, stock, size, color)
└── Variant 3 (SKU, price, stock, size, color)
```

### Product List with Variant Count

```java
@GetMapping
public String list(Model model) {
    List<ProductDto> products = productService.findAll();
    model.addAttribute("products", products);
    return "admin/products/list";
}
```

```html
<td th:text="${product.variants != null ? product.variants.size() : 0}">0</td>
```

### Managing Variants

Variants are managed under a product:

```
/admin/products/{productId}/variants       - List variants
/admin/products/{productId}/variants/new   - Create variant
/admin/products/variants/{id}/view         - View variant
/admin/products/variants/{id}/edit         - Edit variant
/admin/products/variants/{id}/delete       - Delete variant
```

### Image Upload for Variants

```java
@PostMapping("/{productId}/variants/new")
public String createVariant(@PathVariable Long productId,
                            @Valid @ModelAttribute ProductVariantDto dto,
                            @RequestParam(value = "imageFile", required = false) MultipartFile imageFile) {

    // Handle image upload
    if (imageFile != null && !imageFile.isEmpty()) {
        String filename = fileStorageService.store(imageFile);
        dto.setImageUrl("/uploads/" + filename);
    }

    dto.setProductId(productId);
    productVariantService.add(dto);
    return "redirect:/admin/products/" + productId + "/variants";
}
```

---

## Order Management

### Order Statuses

```java
public enum OrderStatus {
    PENDING,    // Just created
    CREATED,    // Confirmed
    PAID,       // Payment received
    SHIPPED,    // Sent to customer
    DELIVERED,  // Received
    CANCELLED   // Cancelled
}
```

### Status Badge Colors

```html
<span class="v-badge"
      th:classappend="${order.status.name() == 'PAID'} ? ' v-badge--ok' :
                      ${order.status.name() == 'CANCELLED'} ? ' v-badge--danger' :
                      ${order.status.name() == 'PENDING'} ? ' v-badge--warning' : ' v-badge--muted'"
      th:text="${order.status}">STATUS</span>
```

### Updating Order Status

```java
@PostMapping("/{id}/status")
public String updateStatus(@PathVariable Long id,
                           @RequestParam OrderStatus status,
                           RedirectAttributes flash) {
    orderService.updateStatus(id, status);
    activityLogService.log("UPDATE", "Order", id, "Order #" + id, "Status → " + status);
    flash.addFlashAttribute("success", "Order status updated!");
    return "redirect:/admin/orders/" + id + "/view";
}
```

---

## Activity Logging

### What Gets Logged

Every admin action is logged:

| Action | Entity | Example |
|--------|--------|---------|
| CREATE | Category | "Created category 'Shirts' (ID: 5)" |
| UPDATE | Product | "Updated product 'Blue Hoodie' (ID: 12)" |
| DELETE | Variant | "Deleted variant 'SKU-001' (ID: 8)" |
| UPDATE | Order | "Updated order status to SHIPPED" |

### Logging Service

```java
@Service
@RequiredArgsConstructor
public class AdminActivityLogServiceImpl implements AdminActivityLogService {

    private final AdminActivityLogRepository repository;

    @Override
    public void log(String action, String entityType, Long entityId, String entityName, String details) {
        AdminActivityLogEntity logEntry = new AdminActivityLogEntity();
        logEntry.setAction(action);
        logEntry.setEntityType(entityType);
        logEntry.setEntityId(entityId);
        logEntry.setEntityName(entityName);
        logEntry.setDescription(generateDescription(action, entityType, entityName, entityId));
        logEntry.setAdminUsername(getCurrentUsername());
        logEntry.setIpAddress(getClientIp());
        logEntry.setTimestamp(LocalDateTime.now());

        repository.save(logEntry);
    }

    private String generateDescription(String action, String entityType, String entityName, Long entityId) {
        String actionVerb = switch (action) {
            case "CREATE" -> "Created";
            case "UPDATE" -> "Updated";
            case "DELETE" -> "Deleted";
            default -> action;
        };

        return String.format("%s %s '%s' (ID: %d)", actionVerb, entityType.toLowerCase(), entityName, entityId);
    }
}
```

### Profile Dropdown with Activity

The admin profile dropdown shows recent activity:

```html
<div class="v-admin-profile__dropdown">
    <div class="v-admin-profile__header">
        <span sec:authentication="name">Admin</span>
    </div>

    <!-- Activity Log Section -->
    <div class="v-admin-profile__section">
        <div class="v-admin-profile__activity-list" id="activityList">
            <!-- Loaded via JavaScript -->
        </div>
    </div>

    <!-- Logout -->
    <form th:action="@{/logout}" method="post">
        <button type="submit">Sign Out</button>
    </form>
</div>
```

```javascript
// Load activity via API
fetch('/api/admin/my-activity')
    .then(response => response.json())
    .then(data => {
        let html = data.map(item => `
            <div class="activity-item">
                <span class="activity-icon ${item.action.toLowerCase()}"></span>
                <span class="activity-text">${item.description}</span>
                <span class="activity-time">${item.timeAgo}</span>
            </div>
        `).join('');
        document.getElementById('activityList').innerHTML = html;
    });
```

---

## Delete Confirmation Modal

All delete actions show a confirmation modal:

```html
<!-- Delete Modal -->
<div id="deleteModal" class="v-modal" style="display:none;">
    <div class="v-modal__content">
        <h3 class="v-modal__title">Confirm Deletion</h3>
        <p class="v-muted">This action cannot be undone.</p>

        <div class="v-kv">
            <div class="v-kv__label">Category</div>
            <div class="v-kv__value" th:text="${category.name}">Name</div>
        </div>

        <div class="v-kv">
            <div class="v-kv__label">ID</div>
            <div class="v-kv__value v-mono" th:text="${category.id}">0</div>
        </div>

        <div class="v-modal__warning">
            Deleting this category will also remove it from all associated products.
        </div>

        <div class="v-modal__actions">
            <button onclick="closeModal()" class="v-btn v-btn--ghost">Cancel</button>
            <form th:action="@{/admin/categories/{id}/delete(id=${category.id})}" method="post">
                <input type="hidden" th:name="${_csrf.parameterName}" th:value="${_csrf.token}"/>
                <button type="submit" class="v-btn v-btn--danger">Delete</button>
            </form>
        </div>
    </div>
</div>

<script>
function openDeleteModal() {
    document.getElementById('deleteModal').style.display = 'flex';
}

function closeModal() {
    document.getElementById('deleteModal').style.display = 'none';
}
</script>
```

---

## Flash Messages

Success/error messages after actions:

```java
// In controller
flash.addFlashAttribute("success", "Category created!");
flash.addFlashAttribute("error", "Failed to delete category");
```

```html
<!-- In layout -->
<div th:if="${success}" class="v-alert v-alert--success" th:text="${success}"></div>
<div th:if="${error}" class="v-alert v-alert--error" th:text="${error}"></div>
```

---

## Admin CSS Classes

| Class | Purpose |
|-------|---------|
| `.v-admin` | Page wrapper |
| `.v-pagehead` | Page header with title and actions |
| `.v-breadcrumb` | Navigation breadcrumb |
| `.v-card` | Content container |
| `.v-table` | Data table |
| `.v-btn` | Button base |
| `.v-btn--primary` | Primary action button |
| `.v-btn--danger` | Destructive action button |
| `.v-btn--ghost` | Secondary/cancel button |
| `.v-badge` | Status badge |
| `.v-modal` | Modal dialog |
| `.v-form__group` | Form field wrapper |
| `.v-input` | Text input |
| `.v-select` | Dropdown select |
| `.v-textarea` | Multi-line input |

---

## Next Steps

- Read [08-STORE-FRONTEND.md](./08-STORE-FRONTEND.md) for customer store
- Or [09-PAYMENT-INTEGRATION.md](./09-PAYMENT-INTEGRATION.md) for Stripe setup
