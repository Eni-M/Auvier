# 09 - Payment Integration

This document explains how Stripe payment processing is integrated into Auvier.

---

## Overview

Auvier uses **Stripe** for secure payment processing. The integration includes:
- Payment Intent API for secure payments
- Stripe Elements for card input
- Webhook handling for payment confirmation

---

## How Stripe Works

```
┌─────────────┐                    ┌─────────────┐                    ┌─────────────┐
│   Client    │                    │   Server    │                    │   Stripe    │
│  (Browser)  │                    │  (Auvier)   │                    │             │
└──────┬──────┘                    └──────┬──────┘                    └──────┬──────┘
       │                                  │                                  │
       │ 1. Checkout form submitted       │                                  │
       │─────────────────────────────────▶│                                  │
       │                                  │                                  │
       │                                  │ 2. Create PaymentIntent          │
       │                                  │─────────────────────────────────▶│
       │                                  │                                  │
       │                                  │ 3. client_secret returned        │
       │                                  │◀─────────────────────────────────│
       │                                  │                                  │
       │ 4. Payment page with client_secret                                  │
       │◀─────────────────────────────────│                                  │
       │                                  │                                  │
       │ 5. User enters card in Stripe Elements                              │
       │                                  │                                  │
       │ 6. stripe.confirmPayment()                                          │
       │─────────────────────────────────────────────────────────────────────▶│
       │                                  │                                  │
       │ 7. Payment processed                                                │
       │◀─────────────────────────────────────────────────────────────────────│
       │                                  │                                  │
       │                                  │ 8. Webhook: payment_intent.succeeded
       │                                  │◀─────────────────────────────────│
       │                                  │                                  │
       │                                  │ 9. Update order status to PAID   │
       │                                  │                                  │
       │ 10. Redirect to success page     │                                  │
       │◀─────────────────────────────────│                                  │
       │                                  │                                  │
```

---

## Configuration

### 1. Get Stripe Keys

1. Go to [Stripe Dashboard](https://dashboard.stripe.com/apikeys)
2. Copy your test keys:
   - **Publishable key** (pk_test_...)
   - **Secret key** (sk_test_...)

### 2. Configure application.properties

```properties
# Stripe Payment Configuration
stripe.api.key=sk_test_YOUR_SECRET_KEY
stripe.public.key=pk_test_YOUR_PUBLISHABLE_KEY
stripe.webhook.secret=whsec_YOUR_WEBHOOK_SECRET
```

### 3. StripeConfig.java

```java
@Configuration
public class StripeConfig {

    @Value("${stripe.api.key}")
    private String apiKey;

    @PostConstruct
    public void init() {
        Stripe.apiKey = apiKey;
    }
}
```

---

## Server-Side Implementation

### PaymentService Interface

```java
public interface PaymentService {
    PaymentIntentDto createPaymentIntent(OrderEntity order);
    boolean confirmPayment(String paymentIntentId);
    void handleWebhookEvent(String payload, String sigHeader);
}
```

### PaymentServiceImpl

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final OrderRepository orderRepository;

    @Value("${stripe.webhook.secret}")
    private String webhookSecret;

    @Override
    public PaymentIntentDto createPaymentIntent(OrderEntity order) {
        try {
            // Convert to cents (Stripe uses smallest currency unit)
            long amountInCents = order.getTotalAmount()
                .multiply(new BigDecimal(100))
                .longValue();

            // Create PaymentIntent
            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(amountInCents)
                .setCurrency("usd")
                .setDescription("Auvier Order #" + order.getId())
                .putMetadata("order_id", order.getId().toString())
                .setAutomaticPaymentMethods(
                    PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                        .setEnabled(true)
                        .build()
                )
                .build();

            PaymentIntent paymentIntent = PaymentIntent.create(params);

            // Save payment intent ID to order
            order.setTransactionId(paymentIntent.getId());
            order.setPaymentStatus("pending");
            orderRepository.save(order);

            log.info("Created PaymentIntent {} for Order {}", paymentIntent.getId(), order.getId());

            return new PaymentIntentDto(
                paymentIntent.getClientSecret(),
                paymentIntent.getId(),
                amountInCents,
                "usd",
                paymentIntent.getStatus()
            );

        } catch (StripeException e) {
            log.error("Stripe error: {}", e.getMessage());
            throw new RuntimeException("Failed to create payment", e);
        }
    }

    @Override
    public boolean confirmPayment(String paymentIntentId) {
        try {
            PaymentIntent paymentIntent = PaymentIntent.retrieve(paymentIntentId);
            return "succeeded".equals(paymentIntent.getStatus());
        } catch (StripeException e) {
            log.error("Error confirming payment: {}", e.getMessage());
            return false;
        }
    }

    @Override
    @Transactional
    public void handleWebhookEvent(String payload, String sigHeader) {
        Event event;
        try {
            event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
        } catch (SignatureVerificationException e) {
            log.error("Invalid webhook signature");
            throw new RuntimeException("Invalid signature");
        }

        log.info("Received Stripe event: {}", event.getType());

        switch (event.getType()) {
            case "payment_intent.succeeded" -> handlePaymentSucceeded(event);
            case "payment_intent.payment_failed" -> handlePaymentFailed(event);
            default -> log.info("Unhandled event: {}", event.getType());
        }
    }

    private void handlePaymentSucceeded(Event event) {
        PaymentIntent paymentIntent = (PaymentIntent) event.getData().getObject();
        String orderId = paymentIntent.getMetadata().get("order_id");

        OrderEntity order = orderRepository.findById(Long.parseLong(orderId))
            .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

        order.setStatus(OrderStatus.PAID);
        order.setPaymentStatus("succeeded");
        orderRepository.save(order);

        log.info("Order {} marked as PAID", orderId);
    }

    private void handlePaymentFailed(Event event) {
        PaymentIntent paymentIntent = (PaymentIntent) event.getData().getObject();
        String orderId = paymentIntent.getMetadata().get("order_id");

        OrderEntity order = orderRepository.findById(Long.parseLong(orderId))
            .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

        order.setPaymentStatus("failed");
        orderRepository.save(order);

        log.warn("Payment failed for Order {}", orderId);
    }
}
```

---

## Webhook Endpoint

### Why Webhooks?

Even after `stripe.confirmPayment()` succeeds on the client, you should wait for the webhook to confirm payment. This handles:
- Network issues between client and your server
- Asynchronous payment methods (3D Secure, bank redirects)
- Fraud detection that might reverse payments

### Controller

```java
@RestController
@RequestMapping("/api/stripe")
@RequiredArgsConstructor
public class StripeWebhookController {

    private final PaymentService paymentService;

    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {

        paymentService.handleWebhookEvent(payload, sigHeader);
        return ResponseEntity.ok("Received");
    }
}
```

### Security Configuration

```java
// In SecurityConfig
.csrf(csrf -> csrf
    .ignoringRequestMatchers("/api/stripe/webhook")  // Stripe sends POST without CSRF token
)
```

### Setting Up Local Webhooks

For local development, use Stripe CLI:

```bash
# Install Stripe CLI
# Windows: scoop install stripe

# Login
stripe login

# Forward webhooks to local server
stripe listen --forward-to localhost:2525/api/stripe/webhook
```

This gives you a webhook secret (`whsec_...`) to put in `application.properties`.

---

## Client-Side Implementation

### Checkout Flow

**1. CheckoutController.java**

```java
@GetMapping("/checkout")
public String checkout(HttpSession session, Model model, Principal principal) {
    List<CartItem> cart = getCart(session);
    if (cart.isEmpty()) {
        return "redirect:/cart";
    }

    UserEntity user = userService.findByUsername(principal.getName()).orElseThrow();
    model.addAttribute("cartItems", cart);
    model.addAttribute("checkoutDto", new CheckoutDto());
    return "store/checkout";
}

@PostMapping("/checkout")
public String processCheckout(@Valid @ModelAttribute CheckoutDto dto,
                              HttpSession session,
                              Principal principal,
                              Model model) {
    List<CartItem> cart = getCart(session);
    UserEntity user = userService.findByUsername(principal.getName()).orElseThrow();

    // Create order
    OrderResponseDto order = orderService.createOrder(user, toOrderCreateDto(cart, dto));

    // Get order entity for payment
    OrderEntity orderEntity = orderRepository.findById(order.getId()).orElseThrow();

    // Create payment intent
    PaymentIntentDto paymentIntent = paymentService.createPaymentIntent(orderEntity);

    model.addAttribute("order", order);
    model.addAttribute("clientSecret", paymentIntent.getClientSecret());
    model.addAttribute("stripePublicKey", stripePublicKey);

    return "store/checkout-payment";
}
```

**2. checkout-payment.html**

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <title>Payment | AUVIER</title>
    <script src="https://js.stripe.com/v3/"></script>
</head>
<body>
    <main class="au-checkout">
        <h1>Complete Payment</h1>

        <div class="au-order-summary">
            <p>Order #<span th:text="${order.id}">123</span></p>
            <p>Total: $<span th:text="${order.totalAmount}">100.00</span></p>
        </div>

        <!-- Stripe Elements Container -->
        <form id="payment-form">
            <div id="payment-element"></div>
            <button id="submit" type="submit" class="au-btn au-btn--primary">
                Pay Now
            </button>
            <div id="error-message"></div>
        </form>
    </main>

    <script th:inline="javascript">
        const stripePublicKey = [[${stripePublicKey}]];
        const clientSecret = [[${clientSecret}]];
        const orderId = [[${order.id}]];
    </script>
    <script src="/assets/public/js/checkout.js"></script>
</body>
</html>
```

**3. checkout.js**

```javascript
// Initialize Stripe
const stripe = Stripe(stripePublicKey);

// Create Elements instance
const elements = stripe.elements({ clientSecret });

// Create and mount Payment Element
const paymentElement = elements.create('payment');
paymentElement.mount('#payment-element');

// Handle form submission
const form = document.getElementById('payment-form');
form.addEventListener('submit', async (event) => {
    event.preventDefault();

    const submitButton = document.getElementById('submit');
    submitButton.disabled = true;
    submitButton.textContent = 'Processing...';

    const { error } = await stripe.confirmPayment({
        elements,
        confirmParams: {
            return_url: window.location.origin + '/checkout/success?order=' + orderId,
        },
    });

    if (error) {
        // Show error to customer
        const errorElement = document.getElementById('error-message');
        errorElement.textContent = error.message;
        submitButton.disabled = false;
        submitButton.textContent = 'Pay Now';
    }
    // If no error, customer is redirected to return_url
});
```

**4. Checkout Success Page**

```java
@GetMapping("/checkout/success")
public String checkoutSuccess(@RequestParam Long order,
                              @RequestParam(required = false) String payment_intent,
                              HttpSession session,
                              Model model) {
    // Verify payment (optional - webhook handles this)
    if (payment_intent != null) {
        boolean paid = paymentService.confirmPayment(payment_intent);
        if (!paid) {
            return "redirect:/checkout/failed?order=" + order;
        }
    }

    // Clear cart
    session.removeAttribute("cart");

    // Show success page
    OrderResponseDto orderDto = orderService.getOrder(order);
    model.addAttribute("order", orderDto);
    return "store/checkout-success";
}
```

---

## Testing

### Test Card Numbers

| Scenario | Card Number |
|----------|-------------|
| Success | 4242 4242 4242 4242 |
| Declined | 4000 0000 0000 0002 |
| Requires Auth | 4000 0025 0000 3155 |
| Insufficient Funds | 4000 0000 0000 9995 |

Use any future expiry date (e.g., 12/34) and any 3-digit CVC.

### Testing Webhooks Locally

```bash
# Terminal 1: Run your app
./mvnw spring-boot:run

# Terminal 2: Forward Stripe webhooks
stripe listen --forward-to localhost:2525/api/stripe/webhook

# Terminal 3: Trigger a test event
stripe trigger payment_intent.succeeded
```

---

## Error Handling

### Common Errors

| Error | Cause | Solution |
|-------|-------|----------|
| `card_declined` | Card declined | Ask customer for different card |
| `expired_card` | Card expired | Ask customer to update card |
| `incorrect_cvc` | Wrong CVC | Ask customer to re-enter |
| `processing_error` | Stripe issue | Retry later |
| `invalid_request_error` | Bad API call | Check your code |

### Handling in JavaScript

```javascript
const { error } = await stripe.confirmPayment({...});

if (error) {
    if (error.type === "card_error" || error.type === "validation_error") {
        showError(error.message);
    } else {
        showError("An unexpected error occurred.");
    }
}
```

---

## Production Checklist

- [ ] Replace test keys with live keys
- [ ] Set up production webhook endpoint
- [ ] Enable HTTPS (required for live mode)
- [ ] Test with real cards in test mode first
- [ ] Set up error monitoring
- [ ] Configure proper error messages
- [ ] Handle refunds (manual or via API)

---

## Next Steps

- Read [10-API-REFERENCE.md](./10-API-REFERENCE.md) for all endpoints
- Or [12-ERROR-HANDLING.md](./12-ERROR-HANDLING.md) for error handling
