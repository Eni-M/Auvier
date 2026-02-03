# 06 - Security & Authentication

This document explains how security works in Auvier, including authentication, authorization, and protection against common attacks.

---

## Overview

Auvier uses **Spring Security 6** for:
- User authentication (login/logout)
- Role-based authorization (ADMIN vs CUSTOMER)
- CSRF protection
- Session management
- Password encryption

---

## Security Configuration

### SecurityConfig.java

```java
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // URL Authorization Rules
            .authorizeHttpRequests(auth -> auth
                // Public static resources
                .requestMatchers("/assets/**", "/uploads/**", "/error", "/favicon.ico").permitAll()

                // Public pages (anyone can view)
                .requestMatchers("/", "/shop", "/shop/**", "/about", "/contact").permitAll()

                // Stripe webhook (special case - no auth, uses signature)
                .requestMatchers("/api/stripe/webhook").permitAll()

                // Admin API requires ADMIN role
                .requestMatchers("/api/admin/**").hasRole("ADMIN")

                // Login/Register only for unauthenticated users
                .requestMatchers("/login", "/register").anonymous()

                // Admin panel requires ADMIN role
                .requestMatchers("/admin/**", "/admin").hasRole("ADMIN")

                // Customer pages require authentication
                .requestMatchers("/account", "/profile", "/cart", "/checkout/**", "/orders/**").authenticated()

                // Everything else is public
                .anyRequest().permitAll()
            )

            // CSRF Configuration
            .csrf(csrf -> csrf
                // Disable CSRF for Stripe webhook (uses signature verification)
                .ignoringRequestMatchers("/api/stripe/webhook")
            )

            // Form Login
            .formLogin(form -> form
                .loginPage("/login")           // Custom login page URL
                .loginProcessingUrl("/login")  // Form POST URL
                .successHandler(customSuccessHandler())  // Where to go after login
            )

            // Logout
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .deleteCookies("JSESSIONID")
            );

        return http.build();
    }

    // Custom success handler - redirect based on role
    @Bean
    public AuthenticationSuccessHandler customSuccessHandler() {
        return (request, response, authentication) -> {
            Set<String> roles = AuthorityUtils.authorityListToSet(authentication.getAuthorities());

            if (roles.contains("ROLE_ADMIN")) {
                response.sendRedirect("/admin");  // Admin goes to dashboard
            } else {
                response.sendRedirect("/");  // Customer goes to store
            }
        };
    }

    // Authentication Manager
    @Bean
    public AuthenticationManager authManager(HttpSecurity http) throws Exception {
        AuthenticationManagerBuilder builder = http.getSharedObject(AuthenticationManagerBuilder.class);
        builder.userDetailsService(userService).passwordEncoder(passwordEncoder);
        return builder.build();
    }
}
```

---

## URL Access Rules Explained

| URL Pattern | Access | Why |
|-------------|--------|-----|
| `/assets/**` | Everyone | CSS, JS need to load |
| `/uploads/**` | Everyone | Product images need to show |
| `/`, `/shop/**` | Everyone | Store must be publicly browsable |
| `/login`, `/register` | Non-authenticated only | Logged-in users don't need these |
| `/admin/**` | ADMIN role only | Sensitive management area |
| `/cart`, `/checkout/**` | Authenticated users | Need to know who's ordering |
| `/profile`, `/orders/**` | Authenticated users | User-specific data |

---

## User Roles

### Role Enum

```java
public enum Role {
    CUSTOMER,  // Regular user - can browse, buy
    ADMIN      // Administrator - full access
}
```

### How Roles Work in Spring Security

When a user logs in, Spring Security creates authorities:

```java
// In UserServiceImpl.loadUserByUsername()
return User.builder()
    .username(user.getUsername())
    .password(user.getPassword())
    .roles(user.getRole().name())  // "ADMIN" becomes "ROLE_ADMIN"
    .build();
```

**Important**: `.roles("ADMIN")` automatically prefixes with `ROLE_`, so:
- `Role.ADMIN` → `ROLE_ADMIN`
- `Role.CUSTOMER` → `ROLE_CUSTOMER`

---

## Password Security

### Password Encoding

```java
@Configuration
public class PasswordConfig {
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

### How BCrypt Works

```java
// When user registers:
String rawPassword = "admin123";
String encodedPassword = passwordEncoder.encode(rawPassword);
// Result: "$2a$10$N9qo8uLOickgx2ZMRZoMy..."

// When user logs in:
boolean matches = passwordEncoder.matches("admin123", encodedPassword);
// Returns: true
```

**BCrypt Features:**
- One-way hash (can't be reversed)
- Includes random salt (same password → different hashes)
- Configurable work factor (slower = more secure)
- Industry standard

---

## Authentication Flow

### Login Process

```
1. User visits /login
   └─→ AuthController returns login.html

2. User submits form (POST /login)
   └─→ Spring Security intercepts

3. Spring Security:
   a. Calls UserService.loadUserByUsername(username)
   b. Compares password with BCrypt
   c. If valid: creates Authentication object
   d. Stores in SecurityContext

4. Success Handler redirects:
   └─→ ADMIN → /admin
   └─→ CUSTOMER → /

5. Subsequent requests:
   └─→ Security filter checks SecurityContext
   └─→ Allows/denies based on URL rules
```

### UserService.loadUserByUsername

```java
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService, UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Find user in database
        UserEntity user = userRepository.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        // Convert to Spring Security UserDetails
        return org.springframework.security.core.userdetails.User.builder()
            .username(user.getUsername())
            .password(user.getPassword())  // BCrypt hash
            .roles(user.getRole().name())  // ADMIN or CUSTOMER
            .build();
    }
}
```

---

## CSRF Protection

### What is CSRF?

Cross-Site Request Forgery - an attacker tricks a logged-in user into making unwanted requests.

### How Spring Security Protects

Every form includes a hidden CSRF token:

```html
<form th:action="@{/admin/categories/new}" method="post">
    <!-- Thymeleaf automatically adds this -->
    <input type="hidden" name="_csrf" value="abc123-token-xyz"/>

    <input type="text" name="name"/>
    <button type="submit">Create</button>
</form>
```

On POST, Spring Security verifies the token matches.

### Manual CSRF Token (when needed)

```html
<!-- In Thymeleaf -->
<input type="hidden" th:name="${_csrf.parameterName}" th:value="${_csrf.token}"/>

<!-- For AJAX requests -->
<meta name="_csrf" th:content="${_csrf.token}"/>
<meta name="_csrf_header" th:content="${_csrf.headerName}"/>
```

```javascript
// JavaScript AJAX with CSRF
const token = document.querySelector('meta[name="_csrf"]').content;
const header = document.querySelector('meta[name="_csrf_header"]').content;

fetch('/api/endpoint', {
    method: 'POST',
    headers: {
        [header]: token,
        'Content-Type': 'application/json'
    },
    body: JSON.stringify(data)
});
```

---

## Session Management

### Configuration

```java
.logout(logout -> logout
    .logoutUrl("/logout")
    .logoutSuccessUrl("/login?logout")
    .invalidateHttpSession(true)    // Destroy session
    .clearAuthentication(true)       // Clear auth object
    .deleteCookies("JSESSIONID")    // Remove session cookie
)
```

### Session Cookie

- **Name**: `JSESSIONID`
- **HttpOnly**: Yes (JavaScript can't read it)
- **Secure**: Should be Yes in production (HTTPS only)

---

## Thymeleaf Security Integration

### Setup

```xml
<!-- pom.xml -->
<dependency>
    <groupId>org.thymeleaf.extras</groupId>
    <artifactId>thymeleaf-extras-springsecurity6</artifactId>
</dependency>
```

```html
<!-- In templates -->
<html xmlns:th="http://www.thymeleaf.org"
      xmlns:sec="http://www.thymeleaf.org/extras/spring-security">
```

### Security Attributes

```html
<!-- Show only if authenticated -->
<div sec:authorize="isAuthenticated()">
    Welcome, <span sec:authentication="name">User</span>!
</div>

<!-- Show only if NOT authenticated -->
<div sec:authorize="!isAuthenticated()">
    <a href="/login">Sign In</a>
</div>

<!-- Show only for ADMIN role -->
<div sec:authorize="hasRole('ADMIN')">
    <a href="/admin">Admin Panel</a>
</div>

<!-- Show for any authenticated user -->
<div sec:authorize="hasAnyRole('ADMIN', 'CUSTOMER')">
    <a href="/profile">My Profile</a>
</div>

<!-- Get current username -->
<span sec:authentication="name">Username</span>

<!-- Get authorities -->
<span sec:authentication="principal.authorities">Roles</span>
```

---

## Getting Current User in Code

### In Controller

```java
@Controller
public class ProfileController {

    // Method 1: Principal parameter
    @GetMapping("/profile")
    public String profile(Principal principal, Model model) {
        String username = principal.getName();
        // ...
    }

    // Method 2: Authentication parameter
    @GetMapping("/profile")
    public String profile(Authentication authentication, Model model) {
        String username = authentication.getName();
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        // ...
    }

    // Method 3: @AuthenticationPrincipal
    @GetMapping("/profile")
    public String profile(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        String username = userDetails.getUsername();
        // ...
    }
}
```

### In Service

```java
@Service
public class OrderServiceImpl {

    public UserEntity getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        return userRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
```

---

## Registration Flow

### AuthController

```java
@Controller
public class AuthController {

    private final UserService userService;

    @GetMapping("/register")
    public String registerForm(Model model) {
        model.addAttribute("userRegistrationDto", new UserRegistrationDto());
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute UserRegistrationDto dto,
                           BindingResult result,
                           RedirectAttributes flash) {
        // Validation errors
        if (result.hasErrors()) {
            return "auth/register";
        }

        // Password confirmation
        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            result.rejectValue("confirmPassword", "error.user", "Passwords do not match");
            return "auth/register";
        }

        try {
            userService.registerUser(dto);
            flash.addFlashAttribute("success", "Registration successful! Please login.");
            return "redirect:/login";
        } catch (DuplicateResourceException e) {
            result.rejectValue("username", "error.user", e.getMessage());
            return "auth/register";
        }
    }
}
```

### UserService.registerUser

```java
@Override
public UserResponseDto registerUser(UserRegistrationDto dto) {
    // Check username uniqueness
    if (userRepository.existsByUsername(dto.getUsername())) {
        throw new DuplicateResourceException("Username already exists");
    }

    // Check email uniqueness
    if (userRepository.existsByEmail(dto.getEmail())) {
        throw new DuplicateResourceException("Email already registered");
    }

    // Create entity
    UserEntity user = registrationMapper.toEntity(dto);
    user.setPassword(passwordEncoder.encode(dto.getPassword()));  // Hash password!
    user.setRole(Role.CUSTOMER);

    // Save and return
    return userMapper.toResponseDto(userRepository.save(user));
}
```

---

## Security Best Practices Used

| Practice | Implementation |
|----------|----------------|
| **Password Hashing** | BCrypt with salt |
| **CSRF Protection** | Automatic token validation |
| **Session Fixation** | New session on login |
| **Secure Cookies** | HttpOnly, Secure flags |
| **Role-Based Access** | `hasRole()` checks |
| **Input Validation** | `@Valid` on DTOs |
| **Error Messages** | Generic "invalid credentials" |

---

## Common Security Scenarios

### Protecting an Endpoint

```java
// In SecurityConfig
.requestMatchers("/api/admin/**").hasRole("ADMIN")

// Or with annotation
@PreAuthorize("hasRole('ADMIN')")
@GetMapping("/api/admin/stats")
public Stats getStats() { ... }
```

### Checking Ownership

```java
public OrderResponseDto getOrderForUser(Long orderId, UserEntity user) {
    OrderEntity order = findOrderById(orderId);

    // Verify user owns this order
    if (!order.getUser().getId().equals(user.getId())) {
        throw new AccessDeniedException("Order does not belong to this user");
    }

    return orderMapper.toResponseDto(order);
}
```

### Conditional UI Based on Role

```html
<!-- Admin sees edit button -->
<a sec:authorize="hasRole('ADMIN')" th:href="@{/admin/products/{id}/edit(id=${product.id})}">
    Edit
</a>

<!-- Customer sees buy button -->
<button sec:authorize="hasRole('CUSTOMER')" type="submit">
    Add to Cart
</button>
```

---

## Next Steps

- Read [07-ADMIN-PANEL.md](./07-ADMIN-PANEL.md) for admin features
- Or [09-PAYMENT-INTEGRATION.md](./09-PAYMENT-INTEGRATION.md) for Stripe setup
