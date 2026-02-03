# 08 - Store Frontend

This document explains the customer-facing store, its pages, and shopping flow.

---

## Overview

The store is a clean, minimalist e-commerce frontend designed for a luxury fashion brand. It uses a light color scheme with elegant typography.

**URL**: `http://localhost:2525/`

---

## Design System

### Color Palette

```css
:root {
    /* Base Colors */
    --au-black: #0a0a0a;
    --au-white: #ffffff;
    --au-gray: #6b6b6b;
    --au-gray-light: #f5f5f5;
    --au-gray-lighter: #fafafa;

    /* Accent */
    --au-accent: #1a1a1a;

    /* Semantic */
    --au-success: #10b981;
    --au-error: #ef4444;
    --au-warning: #f59e0b;
}
```

### Typography

- **Headings**: Cormorant Garamond (serif) - elegant, luxury feel
- **Body**: Montserrat (sans-serif) - clean, readable

```css
.au-hero__title {
    font-family: 'Cormorant Garamond', serif;
    font-size: 4rem;
    font-weight: 300;
    letter-spacing: 0.1em;
}

.au-text {
    font-family: 'Montserrat', sans-serif;
    font-size: 0.875rem;
    line-height: 1.6;
}
```

---

## Page Structure

### Store Layout (_layout.html)

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org"
      xmlns:sec="http://www.thymeleaf.org/extras/spring-security">
<head th:fragment="head(title)">
    <meta charset="UTF-8">
    <title th:text="${title} + ' | AUVIER'">AUVIER</title>
    <link th:href="@{/assets/public/css/public.css}" rel="stylesheet">
</head>

<body>
    <!-- Announcement Bar -->
    <div th:fragment="announcement" class="au-announcement">
        Complimentary shipping on all orders over $500
    </div>

    <!-- Header/Navigation -->
    <header th:fragment="header" class="au-header">
        <nav class="au-nav">
            <!-- Left Nav -->
            <ul class="au-nav__left">
                <li><a href="/shop">Shop</a></li>
                <li><a href="/collections">Collections</a></li>
                <li><a href="/about">About</a></li>
            </ul>

            <!-- Logo (Center) -->
            <a href="/" class="au-logo">AUVIER</a>

            <!-- Right Nav -->
            <ul class="au-nav__right">
                <li><button class="au-nav__icon" onclick="toggleSearch()">🔍</button></li>

                <!-- Not logged in -->
                <li sec:authorize="!isAuthenticated()">
                    <a href="/login">Sign In</a>
                </li>

                <!-- Logged in -->
                <li sec:authorize="isAuthenticated()" class="au-nav__dropdown">
                    <button class="au-nav__icon">👤</button>
                    <div class="au-nav__dropdown-menu">
                        <a href="/profile">My Profile</a>
                        <a href="/orders">My Orders</a>
                        <form action="/logout" method="post">
                            <button type="submit">Sign Out</button>
                        </form>
                    </div>
                </li>

                <!-- Cart -->
                <li>
                    <a href="/cart" class="au-nav__cart">
                        🛒 <span id="cartCount">0</span>
                    </a>
                </li>
            </ul>
        </nav>
    </header>

    <!-- Page Content -->
    <main>
        <th:block th:replace="${content}"/>
    </main>

    <!-- Footer -->
    <footer th:fragment="footer" class="au-footer">
        <!-- Newsletter, Links, Copyright -->
    </footer>

    <script th:src="@{/assets/public/js/public.js}"></script>
</body>
</html>
```

---

## Pages

### 1. Home Page (`/`)

**Controller:**
```java
@GetMapping("/")
public String home(Model model) {
    // Get featured products (latest active products with variants)
    List<ProductDto> featured = productService.findFeaturedProducts(8);
    model.addAttribute("featuredProducts", featured);
    return "store/home";
}
```

**Template (home.html):**
```html
<!-- Hero Section -->
<section class="au-hero">
    <div class="au-hero__content">
        <p class="au-hero__subtitle">Spring/Summer 2026</p>
        <h1 class="au-hero__title">The New Chapter</h1>
        <a href="/shop" class="au-btn">Discover the Collection</a>
    </div>
</section>

<!-- Featured Products -->
<section class="au-section">
    <h2>New Arrivals</h2>
    <div class="au-products">
        <article th:each="product : ${featuredProducts}" class="au-product">
            <a th:href="@{/shop/product/{slug}(slug=${product.slug})}">
                <img th:src="${product.variants[0].imageUrl}" th:alt="${product.name}">
                <h3 th:text="${product.name}">Product Name</h3>
                <p th:text="${'$' + product.variants[0].price}">$299</p>
            </a>
        </article>
    </div>
</section>
```

### 2. Shop Page (`/shop`)

**Controller:**
```java
@GetMapping("/shop")
public String shop(@RequestParam(required = false) Long category,
                   @RequestParam(required = false) String q,
                   Model model) {
    List<ProductDto> products;

    if (category != null) {
        products = productService.findByCategory(category);
    } else if (q != null && !q.isEmpty()) {
        products = productService.search(q);
    } else {
        products = productService.findAllActive();
    }

    model.addAttribute("products", products);
    model.addAttribute("categories", categoryService.findRootCategories());
    return "store/shop";
}
```

**Template (shop.html):**
```html
<section class="au-shop">
    <!-- Sidebar with Categories -->
    <aside class="au-shop__sidebar">
        <h3>Categories</h3>
        <ul>
            <li><a href="/shop">All Products</a></li>
            <li th:each="cat : ${categories}">
                <a th:href="@{/shop(category=${cat.id})}" th:text="${cat.name}">Category</a>
            </li>
        </ul>
    </aside>

    <!-- Product Grid -->
    <div class="au-shop__grid">
        <article th:each="product : ${products}" class="au-product">
            <!-- Product card -->
        </article>

        <!-- Empty state -->
        <div th:if="${#lists.isEmpty(products)}" class="au-shop__empty">
            No products found.
        </div>
    </div>
</section>
```

### 3. Product Detail (`/shop/product/{slug}`)

**Controller:**
```java
@GetMapping("/shop/product/{slug}")
public String product(@PathVariable String slug, Model model) {
    ProductDto product = productService.findBySlug(slug)
        .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

    model.addAttribute("product", product);
    model.addAttribute("relatedProducts", productService.findRelated(product, 4));
    return "store/product";
}
```

**Template (product.html):**
```html
<section class="au-product-detail">
    <!-- Product Images -->
    <div class="au-product-detail__gallery">
        <img th:src="${product.variants[0].imageUrl}" th:alt="${product.name}">
    </div>

    <!-- Product Info -->
    <div class="au-product-detail__info">
        <h1 th:text="${product.name}">Product Name</h1>
        <p class="au-product-detail__price" th:text="${'$' + product.variants[0].price}">$299</p>

        <form th:action="@{/cart/add}" method="post">
            <input type="hidden" name="productId" th:value="${product.id}"/>

            <!-- Size Selection -->
            <div class="au-product-detail__options">
                <label>Size</label>
                <select name="variantId" required>
                    <option th:each="variant : ${product.variants}"
                            th:value="${variant.id}"
                            th:text="${variant.size + ' - ' + variant.color + ' ($' + variant.price + ')'}">
                        Option
                    </option>
                </select>
            </div>

            <!-- Quantity -->
            <div class="au-product-detail__quantity">
                <label>Quantity</label>
                <input type="number" name="quantity" value="1" min="1" max="10">
            </div>

            <!-- Add to Cart -->
            <button type="submit" class="au-btn au-btn--full">Add to Bag</button>
        </form>

        <!-- Description -->
        <div class="au-product-detail__description" th:utext="${product.description}">
            Description here...
        </div>
    </div>
</section>
```

### 4. Cart (`/cart`)

**Controller:**
```java
@GetMapping("/cart")
public String cart(HttpSession session, Model model) {
    @SuppressWarnings("unchecked")
    List<CartItem> cartItems = (List<CartItem>) session.getAttribute("cart");

    if (cartItems == null) {
        cartItems = new ArrayList<>();
    }

    BigDecimal total = cartItems.stream()
        .map(CartItem::getSubtotal)
        .reduce(BigDecimal.ZERO, BigDecimal::add);

    model.addAttribute("cartItems", cartItems);
    model.addAttribute("total", total);
    return "store/cart";
}

@PostMapping("/cart/add")
public String addToCart(@RequestParam Long variantId,
                        @RequestParam(defaultValue = "1") int quantity,
                        HttpSession session,
                        RedirectAttributes flash) {
    // Get or create cart
    @SuppressWarnings("unchecked")
    List<CartItem> cart = (List<CartItem>) session.getAttribute("cart");
    if (cart == null) {
        cart = new ArrayList<>();
        session.setAttribute("cart", cart);
    }

    // Add item
    ProductVariantDto variant = productVariantService.findOne(variantId);
    cart.add(new CartItem(variant, quantity));

    flash.addFlashAttribute("success", "Added to bag!");
    return "redirect:/cart";
}
```

**Cart Item Class:**
```java
@Data
@AllArgsConstructor
public class CartItem {
    private ProductVariantDto variant;
    private int quantity;

    public BigDecimal getSubtotal() {
        return variant.getPrice().multiply(BigDecimal.valueOf(quantity));
    }
}
```

### 5. Checkout (`/checkout`)

**Controller:**
```java
@GetMapping("/checkout")
public String checkout(HttpSession session, Model model, Principal principal) {
    // Verify cart has items
    List<CartItem> cart = getCart(session);
    if (cart.isEmpty()) {
        return "redirect:/cart";
    }

    // Get user
    UserEntity user = userService.findByUsername(principal.getName())
        .orElseThrow();

    model.addAttribute("cartItems", cart);
    model.addAttribute("user", user);
    model.addAttribute("checkoutDto", new CheckoutDto());
    return "store/checkout";
}

@PostMapping("/checkout")
public String processCheckout(@Valid @ModelAttribute CheckoutDto dto,
                              BindingResult result,
                              HttpSession session,
                              Principal principal,
                              Model model) {
    if (result.hasErrors()) {
        return "store/checkout";
    }

    List<CartItem> cart = getCart(session);
    UserEntity user = userService.findByUsername(principal.getName()).orElseThrow();

    // Create order
    OrderCreateDto orderDto = new OrderCreateDto();
    orderDto.setShippingAddress(dto.getShippingAddress());
    orderDto.setItems(cart.stream()
        .map(item -> new OrderItemCreateDto(item.getVariant().getId(), item.getQuantity()))
        .toList());

    OrderResponseDto order = orderService.createOrder(user, orderDto);

    // Create payment intent
    PaymentIntentDto paymentIntent = paymentService.createPaymentIntent(order);

    model.addAttribute("order", order);
    model.addAttribute("clientSecret", paymentIntent.getClientSecret());
    model.addAttribute("stripePublicKey", stripePublicKey);

    return "store/checkout-payment";
}
```

---

## Shopping Flow

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│   Browse    │────▶│   Product   │────▶│  Add to     │
│   Shop      │     │   Detail    │     │  Cart       │
└─────────────┘     └─────────────┘     └──────┬──────┘
                                               │
                                               ▼
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│   Order     │◀────│   Payment   │◀────│  Checkout   │
│   Success   │     │   (Stripe)  │     │  Form       │
└─────────────┘     └─────────────┘     └─────────────┘
                                               │
                                        ┌──────┴──────┐
                                        │ Must be     │
                                        │ logged in   │
                                        └─────────────┘
```

---

## User Account Pages

### Profile (`/profile`)

```java
@GetMapping("/profile")
public String profile(Principal principal, Model model) {
    UserEntity user = userService.findByUsername(principal.getName()).orElseThrow();
    model.addAttribute("user", user);
    return "store/profile";  // or "auth/profile"
}

@PostMapping("/profile")
public String updateProfile(@Valid @ModelAttribute UserRegistrationDto dto,
                            Principal principal,
                            RedirectAttributes flash) {
    UserEntity user = userService.findByUsername(principal.getName()).orElseThrow();
    userService.updateUser(user.getId(), dto);
    flash.addFlashAttribute("success", "Profile updated!");
    return "redirect:/profile";
}
```

### Order History (`/orders`)

```java
@GetMapping("/orders")
public String orders(Principal principal, Model model) {
    UserEntity user = userService.findByUsername(principal.getName()).orElseThrow();
    List<OrderSummaryDto> orders = orderService.getOrdersForUser(user);
    model.addAttribute("orders", orders);
    return "store/orders";
}

@GetMapping("/orders/{id}")
public String orderDetail(@PathVariable Long id, Principal principal, Model model) {
    UserEntity user = userService.findByUsername(principal.getName()).orElseThrow();
    OrderResponseDto order = orderService.getOrderForUser(id, user);
    model.addAttribute("order", order);
    return "store/order-detail";
}
```

---

## CSS Classes (Store)

| Class | Purpose |
|-------|---------|
| `.au-container` | Centered container with max-width |
| `.au-hero` | Full-width hero section |
| `.au-section` | Page section with padding |
| `.au-products` | Product grid |
| `.au-product` | Product card |
| `.au-btn` | Button base |
| `.au-btn--primary` | Primary CTA button |
| `.au-btn--secondary` | Secondary button |
| `.au-form__group` | Form field wrapper |
| `.au-input` | Text input |
| `.au-select` | Dropdown |

---

## JavaScript Features

### Cart Counter

```javascript
// Update cart count in header
function updateCartCount() {
    fetch('/api/cart/count')
        .then(response => response.json())
        .then(data => {
            const counter = document.getElementById('cartCount');
            if (data.count > 0) {
                counter.textContent = data.count;
                counter.style.display = 'flex';
            } else {
                counter.style.display = 'none';
            }
        });
}

// Call on page load
document.addEventListener('DOMContentLoaded', updateCartCount);
```

### Search Overlay

```javascript
function toggleSearch() {
    const overlay = document.getElementById('searchOverlay');
    overlay.classList.toggle('active');
    if (overlay.classList.contains('active')) {
        overlay.querySelector('input').focus();
    }
}
```

### Account Dropdown

```javascript
function toggleAccountMenu() {
    const dropdown = document.getElementById('accountDropdown');
    dropdown.classList.toggle('active');
}

// Close on click outside
document.addEventListener('click', (e) => {
    if (!e.target.closest('.au-nav__dropdown')) {
        document.getElementById('accountDropdown')?.classList.remove('active');
    }
});
```

---

## Responsive Design

```css
/* Mobile: Stack navigation */
@media (max-width: 768px) {
    .au-nav {
        flex-direction: column;
    }

    .au-nav__left,
    .au-nav__right {
        display: none;
    }

    .au-nav__toggle {
        display: block;
    }

    .au-products {
        grid-template-columns: repeat(2, 1fr);
    }
}

/* Tablet: 2-3 column grid */
@media (min-width: 769px) and (max-width: 1024px) {
    .au-products {
        grid-template-columns: repeat(3, 1fr);
    }
}

/* Desktop: 4 column grid */
@media (min-width: 1025px) {
    .au-products {
        grid-template-columns: repeat(4, 1fr);
    }
}
```

---

## Next Steps

- Read [09-PAYMENT-INTEGRATION.md](./09-PAYMENT-INTEGRATION.md) for Stripe setup
- Or [10-API-REFERENCE.md](./10-API-REFERENCE.md) for all endpoints
