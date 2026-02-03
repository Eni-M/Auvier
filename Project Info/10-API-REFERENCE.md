# 10 - API Reference

This document lists all HTTP endpoints in the Auvier application.

---

## Public Store Endpoints

### Home & Static Pages

| Method | URL | Controller | Description |
|--------|-----|------------|-------------|
| GET | `/` | HomeController | Store home page |
| GET | `/about` | StoreController | About page |
| GET | `/contact` | StoreController | Contact page |
| GET | `/faq` | StoreController | FAQ page |
| GET | `/shipping` | StoreController | Shipping info |
| GET | `/size-guide` | StoreController | Size guide |
| GET | `/collections` | StoreController | Collections page |

### Shop & Products

| Method | URL | Controller | Description |
|--------|-----|------------|-------------|
| GET | `/shop` | StoreController | Product listing |
| GET | `/shop?category={id}` | StoreController | Filter by category |
| GET | `/shop?q={search}` | StoreController | Search products |
| GET | `/shop/product/{slug}` | StoreController | Product detail |

---

## Authentication Endpoints

| Method | URL | Controller | Description |
|--------|-----|------------|-------------|
| GET | `/login` | AuthController | Login page |
| POST | `/login` | Spring Security | Process login |
| GET | `/register` | AuthController | Registration page |
| POST | `/register` | AuthController | Process registration |
| POST | `/logout` | Spring Security | Process logout |

### Registration Request Body

```html
<form action="/register" method="post">
    <input name="username" type="text" required>
    <input name="email" type="email" required>
    <input name="password" type="password" required>
    <input name="confirmPassword" type="password" required>
    <input name="firstName" type="text">
    <input name="lastName" type="text">
    <input name="_csrf" type="hidden" value="...">
</form>
```

---

## Customer Account Endpoints

*Requires authentication*

| Method | URL | Controller | Description |
|--------|-----|------------|-------------|
| GET | `/profile` | AuthController | User profile |
| POST | `/profile` | AuthController | Update profile |
| GET | `/orders` | CustomerOrderController | Order history |
| GET | `/orders/{id}` | CustomerOrderController | Order detail |

---

## Cart Endpoints

| Method | URL | Controller | Description |
|--------|-----|------------|-------------|
| GET | `/cart` | CheckoutController | View cart |
| POST | `/cart/add` | CheckoutController | Add item to cart |
| POST | `/cart/update` | CheckoutController | Update quantity |
| POST | `/cart/remove` | CheckoutController | Remove item |
| GET | `/api/cart/count` | CheckoutController | Get cart item count (JSON) |

### Add to Cart Request

```html
<form action="/cart/add" method="post">
    <input name="variantId" type="hidden" value="123">
    <input name="quantity" type="number" value="1" min="1">
    <input name="_csrf" type="hidden" value="...">
</form>
```

---

## Checkout Endpoints

*Requires authentication*

| Method | URL | Controller | Description |
|--------|-----|------------|-------------|
| GET | `/checkout` | CheckoutController | Checkout form |
| POST | `/checkout` | CheckoutController | Submit order, get payment page |
| GET | `/checkout/success` | CheckoutController | Order success page |
| GET | `/checkout/failed` | CheckoutController | Payment failed page |

---

## Admin Panel Endpoints

*Requires ADMIN role*

### Dashboard

| Method | URL | Controller | Description |
|--------|-----|------------|-------------|
| GET | `/admin` | AdminController | Admin dashboard |

### Categories

| Method | URL | Controller | Description |
|--------|-----|------------|-------------|
| GET | `/admin/categories` | CategoryController | List all categories |
| GET | `/admin/categories/{id}/view` | CategoryController | View category |
| GET | `/admin/categories/new` | CategoryController | New category form |
| POST | `/admin/categories/new` | CategoryController | Create category |
| GET | `/admin/categories/{id}/edit` | CategoryController | Edit category form |
| POST | `/admin/categories/{id}/edit` | CategoryController | Update category |
| POST | `/admin/categories/{id}/delete` | CategoryController | Delete category |

### Products

| Method | URL | Controller | Description |
|--------|-----|------------|-------------|
| GET | `/admin/products` | ProductController | List all products |
| GET | `/admin/products/{id}/view` | ProductController | View product |
| GET | `/admin/products/new` | ProductController | New product form |
| POST | `/admin/products/new` | ProductController | Create product |
| GET | `/admin/products/{id}/edit` | ProductController | Edit product form |
| POST | `/admin/products/{id}/edit` | ProductController | Update product |
| POST | `/admin/products/{id}/delete` | ProductController | Delete product |

### Product Variants

| Method | URL | Controller | Description |
|--------|-----|------------|-------------|
| GET | `/admin/products/{id}/variants` | ProductVariantAdminController | List variants |
| GET | `/admin/products/{id}/variants/new` | ProductVariantAdminController | New variant form |
| POST | `/admin/products/{id}/variants/new` | ProductVariantAdminController | Create variant |
| GET | `/admin/products/variants/{id}/view` | ProductVariantAdminController | View variant |
| GET | `/admin/products/variants/{id}/edit` | ProductVariantAdminController | Edit variant form |
| POST | `/admin/products/variants/{id}/edit` | ProductVariantAdminController | Update variant |
| POST | `/admin/products/variants/{id}/delete` | ProductVariantAdminController | Delete variant |

### Orders

| Method | URL | Controller | Description |
|--------|-----|------------|-------------|
| GET | `/admin/orders` | OrderController | List all orders |
| GET | `/admin/orders/{id}/view` | OrderController | View order |
| POST | `/admin/orders/{id}/status` | OrderController | Update order status |
| POST | `/admin/orders/{id}/delete` | OrderController | Delete order |

### Users

| Method | URL | Controller | Description |
|--------|-----|------------|-------------|
| GET | `/admin/users` | UserController | List all users |
| GET | `/admin/users/{id}/view` | UserController | View user |
| GET | `/admin/users/new` | UserController | New user form |
| POST | `/admin/users/new` | UserController | Create user |
| GET | `/admin/users/{id}/edit` | UserController | Edit user form |
| POST | `/admin/users/{id}/edit` | UserController | Update user |
| POST | `/admin/users/{id}/delete` | UserController | Delete user |

### Activity Log

| Method | URL | Controller | Description |
|--------|-----|------------|-------------|
| GET | `/admin/activity` | AdminActivityLogController | View activity log |

---

## Admin API Endpoints (JSON)

*Requires ADMIN role*

| Method | URL | Controller | Description |
|--------|-----|------------|-------------|
| GET | `/api/admin/my-activity` | AdminActivityLogController | Get current admin's activity |

### Response Format

```json
[
    {
        "id": 1,
        "action": "CREATE",
        "entityType": "Category",
        "entityId": 5,
        "entityName": "Shirts",
        "description": "Created category 'Shirts' (ID: 5)",
        "adminUsername": "admin",
        "timestamp": "2026-02-03T10:30:00",
        "timeAgo": "2 hours ago"
    }
]
```

---

## Stripe Webhook Endpoint

| Method | URL | Controller | Description |
|--------|-----|------------|-------------|
| POST | `/api/stripe/webhook` | StripeWebhookController | Stripe webhook handler |

*Note: This endpoint is public but verified via Stripe signature*

---

## Error Pages

| URL | Template | Description |
|-----|----------|-------------|
| `/error` (400) | `error/400.html` | Bad Request |
| `/error` (403) | `error/403.html` | Access Denied |
| `/error` (404) | `error/404.html` | Not Found |
| `/error` (500) | `error/500.html` | Server Error |

---

## Request/Response Examples

### Create Category

**Request:**
```http
POST /admin/categories/new HTTP/1.1
Content-Type: application/x-www-form-urlencoded

name=Shirts&slug=shirts&parentId=1&active=true&description=All+shirts&_csrf=abc123
```

**Response:**
```http
HTTP/1.1 302 Found
Location: /admin/categories
```

### Create Product

**Request:**
```http
POST /admin/products/new HTTP/1.1
Content-Type: application/x-www-form-urlencoded

name=Blue+Hoodie&slug=blue-hoodie&description=A+warm+hoodie&categoryId=1&subCategoryId=2&active=true&_csrf=abc123
```

### Create Variant (with image)

**Request:**
```http
POST /admin/products/1/variants/new HTTP/1.1
Content-Type: multipart/form-data

sku=HOODIE-BLU-M&price=49.99&stock=100&color=Blue&size=M&active=true&imageFile=@image.jpg&_csrf=abc123
```

### Update Order Status

**Request:**
```http
POST /admin/orders/123/status HTTP/1.1
Content-Type: application/x-www-form-urlencoded

status=SHIPPED&_csrf=abc123
```

---

## URL Parameters

### Pagination (where implemented)

| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| `page` | int | 0 | Page number (0-indexed) |
| `size` | int | 20 | Items per page |
| `sort` | string | varies | Sort field and direction |

### Filtering

| Parameter | Type | Description |
|-----------|------|-------------|
| `category` | Long | Filter by category ID |
| `q` | String | Search query |
| `status` | String | Filter by order status |

---

## HTTP Status Codes Used

| Code | Meaning | When Used |
|------|---------|-----------|
| 200 | OK | Successful GET, successful form submission returning same page |
| 302 | Found (Redirect) | After successful POST (PRG pattern) |
| 400 | Bad Request | Validation errors, malformed request |
| 401 | Unauthorized | Not logged in (redirects to login) |
| 403 | Forbidden | Logged in but no permission |
| 404 | Not Found | Resource doesn't exist |
| 500 | Server Error | Unexpected error |

---

## Next Steps

- Read [11-FRONTEND-GUIDE.md](./11-FRONTEND-GUIDE.md) for CSS/JS details
- Or [12-ERROR-HANDLING.md](./12-ERROR-HANDLING.md) for error handling
