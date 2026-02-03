# 11 - Frontend Guide

This document explains the frontend architecture including CSS, JavaScript, and Thymeleaf templates.

---

## File Structure

```
src/main/resources/
├── static/
│   ├── favicon.ico
│   └── assets/
│       ├── admin/
│       │   ├── css/admin.css     # Admin panel styles
│       │   └── js/admin.js       # Admin panel scripts
│       └── public/
│           ├── css/public.css    # Store styles
│           └── js/public.js      # Store scripts
└── templates/
    ├── admin/                    # Admin templates
    ├── store/                    # Store templates
    ├── auth/                     # Login/register
    ├── error/                    # Error pages
    └── fragments/                # Shared fragments
```

---

## CSS Architecture

### Naming Convention: BEM-like

```css
/* Block */
.au-product { }

/* Element (part of block) */
.au-product__image { }
.au-product__title { }
.au-product__price { }

/* Modifier (variant of block/element) */
.au-product--featured { }
.au-btn--primary { }
.au-btn--danger { }
```

### CSS Custom Properties (Variables)

```css
/* Store (public.css) */
:root {
    /* Colors */
    --au-black: #0a0a0a;
    --au-white: #ffffff;
    --au-gray: #6b6b6b;
    --au-gray-light: #f5f5f5;

    /* Spacing */
    --au-space-xs: 0.25rem;
    --au-space-sm: 0.5rem;
    --au-space-md: 1rem;
    --au-space-lg: 1.5rem;
    --au-space-xl: 2rem;
    --au-space-2xl: 3rem;
    --au-space-3xl: 4rem;

    /* Typography */
    --au-font-serif: 'Cormorant Garamond', serif;
    --au-font-sans: 'Montserrat', sans-serif;

    /* Transitions */
    --au-transition: 0.2s ease;
}

/* Admin (admin.css) */
:root {
    --admin-bg: #0d0d0f;
    --admin-surface: #141418;
    --admin-border: rgba(255, 255, 255, 0.08);
    --admin-text: rgba(255, 255, 255, 0.92);
    --admin-text-muted: rgba(255, 255, 255, 0.5);
    --admin-primary: #3b82f6;
    --admin-success: #10b981;
    --admin-danger: #ef4444;
}
```

---

## Admin Panel Styles

### Layout

```css
/* Page structure */
.v-admin-body {
    background: var(--admin-bg);
    color: var(--admin-text);
    min-height: 100vh;
}

/* Top navigation bar */
.v-topbar {
    background: var(--admin-surface);
    border-bottom: 1px solid var(--admin-border);
    padding: 0 24px;
    height: 64px;
}

/* Main content area */
.v-main {
    padding: 32px;
    max-width: 1400px;
    margin: 0 auto;
}
```

### Cards

```css
.v-card {
    background: var(--admin-surface);
    border: 1px solid var(--admin-border);
    border-radius: 12px;
}

.v-card__header {
    padding: 20px 24px;
    border-bottom: 1px solid var(--admin-border);
}

.v-card__body {
    padding: 24px;
}
```

### Buttons

```css
.v-btn {
    display: inline-flex;
    align-items: center;
    gap: 8px;
    padding: 10px 20px;
    border-radius: 8px;
    font-size: 13px;
    font-weight: 500;
    cursor: pointer;
    transition: all var(--au-transition);
}

.v-btn--primary {
    background: var(--admin-primary);
    color: white;
}

.v-btn--danger {
    background: var(--admin-danger);
    color: white;
}

.v-btn--ghost {
    background: transparent;
    border: 1px solid var(--admin-border);
    color: var(--admin-text);
}
```

### Tables

```css
.v-table {
    width: 100%;
    border-collapse: collapse;
}

.v-table th,
.v-table td {
    padding: 14px 16px;
    text-align: left;
    border-bottom: 1px solid var(--admin-border);
}

.v-table tr:hover {
    background: var(--admin-surface-hover);
}
```

### Forms

```css
.v-form__group {
    margin-bottom: 20px;
}

.v-label {
    display: block;
    margin-bottom: 8px;
    font-size: 13px;
    color: var(--admin-text-muted);
}

.v-input,
.v-select,
.v-textarea {
    width: 100%;
    padding: 12px 16px;
    background: rgba(255, 255, 255, 0.04);
    border: 1px solid var(--admin-border);
    border-radius: 8px;
    color: var(--admin-text);
}

.v-input:focus {
    outline: none;
    border-color: var(--admin-primary);
}
```

---

## Store Styles

### Navigation

```css
.au-header {
    position: sticky;
    top: 0;
    background: var(--au-white);
    z-index: 100;
    border-bottom: 1px solid var(--au-gray-light);
}

.au-nav {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 1rem 2rem;
}

.au-logo {
    font-family: var(--au-font-serif);
    font-size: 1.5rem;
    letter-spacing: 0.2em;
}
```

### Product Grid

```css
.au-products {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 2rem;
}

.au-product {
    text-align: center;
}

.au-product__image {
    width: 100%;
    aspect-ratio: 3/4;
    object-fit: cover;
}

.au-product__name {
    font-family: var(--au-font-serif);
    font-size: 1.125rem;
    margin-top: 1rem;
}

.au-product__price {
    color: var(--au-gray);
    margin-top: 0.5rem;
}
```

### Buttons

```css
.au-btn {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    padding: 1rem 2rem;
    font-size: 0.75rem;
    font-weight: 500;
    letter-spacing: 0.1em;
    text-transform: uppercase;
    border: 1px solid var(--au-black);
    background: var(--au-black);
    color: var(--au-white);
    transition: all var(--au-transition);
}

.au-btn:hover {
    background: transparent;
    color: var(--au-black);
}

.au-btn--secondary {
    background: transparent;
    color: var(--au-black);
}

.au-btn--secondary:hover {
    background: var(--au-black);
    color: var(--au-white);
}
```

---

## JavaScript

### Admin Panel (admin.js)

```javascript
// ============ Modal Functions ============
function openDeleteModal() {
    document.getElementById('deleteModal').style.display = 'flex';
}

function closeModal() {
    document.querySelectorAll('.v-modal').forEach(modal => {
        modal.style.display = 'none';
    });
}

// Close on backdrop click
document.addEventListener('click', (e) => {
    if (e.target.classList.contains('v-modal')) {
        closeModal();
    }
});

// Close on Escape key
document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape') closeModal();
});


// ============ Profile Dropdown ============
function toggleProfileDropdown() {
    const dropdown = document.querySelector('.v-admin-profile__dropdown');
    dropdown.classList.toggle('active');
}

// Close dropdown on outside click
document.addEventListener('click', (e) => {
    if (!e.target.closest('.v-admin-profile')) {
        document.querySelector('.v-admin-profile__dropdown')?.classList.remove('active');
    }
});


// ============ Activity Log ============
async function loadActivityLog() {
    try {
        const response = await fetch('/api/admin/my-activity');
        const activities = await response.json();

        const container = document.getElementById('activityList');
        if (!container) return;

        container.innerHTML = activities.map(item => `
            <div class="v-admin-profile__activity-item">
                <span class="v-admin-profile__activity-icon ${item.action.toLowerCase()}">
                    ${getActionIcon(item.action)}
                </span>
                <div class="v-admin-profile__activity-content">
                    <span class="v-admin-profile__activity-text">${item.description}</span>
                    <span class="v-admin-profile__activity-time">${item.timeAgo}</span>
                </div>
            </div>
        `).join('');
    } catch (error) {
        console.error('Failed to load activity:', error);
    }
}

function getActionIcon(action) {
    switch (action) {
        case 'CREATE': return '+';
        case 'UPDATE': return '✎';
        case 'DELETE': return '×';
        default: return '•';
    }
}

// Load activity when page loads
document.addEventListener('DOMContentLoaded', loadActivityLog);


// ============ Image Preview ============
function previewImage(input, previewId) {
    const preview = document.getElementById(previewId);
    if (input.files && input.files[0]) {
        const reader = new FileReader();
        reader.onload = (e) => {
            preview.src = e.target.result;
            preview.style.display = 'block';
        };
        reader.readAsDataURL(input.files[0]);
    }
}
```

### Store (public.js)

```javascript
// ============ Search Overlay ============
function toggleSearch() {
    const overlay = document.getElementById('searchOverlay');
    overlay.classList.toggle('active');

    if (overlay.classList.contains('active')) {
        overlay.querySelector('input').focus();
    }
}


// ============ Mobile Menu ============
function toggleMobileMenu() {
    const menu = document.getElementById('mobileMenu');
    menu.classList.toggle('active');
    document.body.classList.toggle('menu-open');
}


// ============ Account Dropdown ============
function toggleAccountMenu() {
    const dropdown = document.getElementById('accountDropdown');
    dropdown.classList.toggle('active');
}

// Close on outside click
document.addEventListener('click', (e) => {
    if (!e.target.closest('.au-nav__dropdown')) {
        document.getElementById('accountDropdown')?.classList.remove('active');
    }
});


// ============ Cart Functions ============
async function updateCartCount() {
    try {
        const response = await fetch('/api/cart/count');
        const data = await response.json();

        const counter = document.getElementById('cartCount');
        if (counter) {
            if (data.count > 0) {
                counter.textContent = data.count;
                counter.style.display = 'flex';
            } else {
                counter.style.display = 'none';
            }
        }
    } catch (error) {
        console.error('Failed to update cart count:', error);
    }
}

// Update cart count on page load
document.addEventListener('DOMContentLoaded', updateCartCount);


// ============ Quantity Selector ============
function updateQuantity(input, delta) {
    const newValue = parseInt(input.value) + delta;
    if (newValue >= 1 && newValue <= 99) {
        input.value = newValue;
    }
}
```

---

## Thymeleaf Templates

### Layout Pattern

```html
<!-- _layout.html (layout file) -->
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org"
      th:fragment="layout(title, content, styles, scripts)">
<head>
    <title th:text="${title} + ' | AUVIER'">AUVIER</title>
    <link th:href="@{/assets/public/css/public.css}" rel="stylesheet">
    <th:block th:replace="${styles}" />
</head>
<body>
    <!-- Header -->
    <header th:replace="~{store/fragments/_layout :: header}"></header>

    <!-- Content -->
    <main>
        <th:block th:replace="${content}" />
    </main>

    <!-- Footer -->
    <footer th:replace="~{store/fragments/_layout :: footer}"></footer>

    <script th:src="@{/assets/public/js/public.js}"></script>
    <th:block th:replace="${scripts}" />
</body>
</html>

<!-- page.html (using layout) -->
<!DOCTYPE html>
<html th:replace="~{store/fragments/_layout :: layout('Page Title', ~{::main}, null, null)}">
<main>
    <h1>Page Content</h1>
</main>
</html>
```

### Common Thymeleaf Patterns

```html
<!-- Conditional Display -->
<div th:if="${products != null and !products.isEmpty()}">
    <!-- Content when products exist -->
</div>
<div th:unless="${products != null and !products.isEmpty()}">
    No products found.
</div>

<!-- Iteration -->
<div th:each="product : ${products}">
    <span th:text="${product.name}">Product</span>
</div>

<!-- Iteration with index -->
<div th:each="item, stat : ${items}">
    <span th:text="${stat.index}">0</span> <!-- 0-based index -->
    <span th:text="${stat.count}">1</span> <!-- 1-based count -->
    <span th:text="${stat.first}">true</span>
    <span th:text="${stat.last}">false</span>
</div>

<!-- URL Building -->
<a th:href="@{/products/{id}(id=${product.id})}">View</a>
<a th:href="@{/shop(category=${cat.id}, sort='price')}">Filter</a>

<!-- Form Binding -->
<form th:action="@{/admin/products/new}" th:object="${productDto}" method="post">
    <input th:field="*{name}" type="text">
    <span th:if="${#fields.hasErrors('name')}" th:errors="*{name}">Error</span>
</form>

<!-- Fragment Inclusion -->
<div th:replace="~{fragments/header :: header}"></div>
<div th:insert="~{fragments/footer :: footer}"></div>

<!-- Security -->
<div sec:authorize="isAuthenticated()">Logged in</div>
<div sec:authorize="!isAuthenticated()">Not logged in</div>
<div sec:authorize="hasRole('ADMIN')">Admin only</div>
<span sec:authentication="name">Username</span>

<!-- Formatting -->
<span th:text="${#numbers.formatDecimal(price, 1, 2)}">0.00</span>
<span th:text="${#temporals.format(date, 'MMM dd, yyyy')}">Jan 01, 2026</span>

<!-- Conditional Classes -->
<span th:classappend="${active} ? 'active' : 'inactive'">Status</span>
<div th:class="${large} ? 'big-card' : 'small-card'">Card</div>

<!-- Safe HTML Output -->
<div th:utext="${product.description}">HTML content</div>  <!-- Unescaped -->
<div th:text="${product.description}">Text content</div>   <!-- Escaped (safe) -->
```

### Fragment Pattern

```html
<!-- fragments/product-card.html -->
<article th:fragment="card(product)" class="au-product">
    <a th:href="@{/shop/product/{slug}(slug=${product.slug})}">
        <img th:src="${product.imageUrl}" th:alt="${product.name}">
        <h3 th:text="${product.name}">Name</h3>
        <p th:text="${'$' + product.price}">$0</p>
    </a>
</article>

<!-- Using the fragment -->
<div class="au-products">
    <th:block th:each="product : ${products}">
        <article th:replace="~{fragments/product-card :: card(${product})}"></article>
    </th:block>
</div>
```

---

## Responsive Design

### Breakpoints

```css
/* Mobile first approach */

/* Small devices (phones) */
@media (max-width: 576px) { }

/* Medium devices (tablets) */
@media (min-width: 577px) and (max-width: 768px) { }

/* Large devices (desktops) */
@media (min-width: 769px) and (max-width: 1024px) { }

/* Extra large devices */
@media (min-width: 1025px) { }
```

### Responsive Product Grid

```css
.au-products {
    display: grid;
    gap: 1.5rem;
    grid-template-columns: repeat(2, 1fr);  /* Mobile: 2 columns */
}

@media (min-width: 769px) {
    .au-products {
        grid-template-columns: repeat(3, 1fr);  /* Tablet: 3 columns */
    }
}

@media (min-width: 1025px) {
    .au-products {
        grid-template-columns: repeat(4, 1fr);  /* Desktop: 4 columns */
    }
}
```

---

## Next Steps

- Read [12-ERROR-HANDLING.md](./12-ERROR-HANDLING.md) for error handling
- Or [13-TESTING-GUIDE.md](./13-TESTING-GUIDE.md) for testing
