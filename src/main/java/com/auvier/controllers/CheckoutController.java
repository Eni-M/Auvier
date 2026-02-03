package com.auvier.controllers;

import com.auvier.dtos.CheckoutDto;
import com.auvier.dtos.PaymentIntentDto;
import com.auvier.entities.OrderEntity;
import com.auvier.entities.OrderItemEntity;
import com.auvier.entities.UserEntity;
import com.auvier.enums.OrderStatus;
import com.auvier.infrastructure.services.PaymentService;
import com.auvier.infrastructure.services.ProductVariantService;
import com.auvier.infrastructure.services.UserService;
import com.auvier.repositories.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@Slf4j
public class CheckoutController {

    private final PaymentService paymentService;
    private final OrderRepository orderRepository;
    private final UserService userService;
    private final ProductVariantService productVariantService;

    @Value("${stripe.public.key}")
    private String stripePublicKey;

    @GetMapping("/checkout")
    public String checkout(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        model.addAttribute("stripePublicKey", stripePublicKey);
        model.addAttribute("checkoutDto", new CheckoutDto());
        return "store/checkout";
    }

    @PostMapping("/checkout/create-order")
    @ResponseBody
    public ResponseEntity<?> createOrder(
            @RequestBody CheckoutDto checkoutDto,
            @AuthenticationPrincipal UserDetails userDetails) {

        try {
            // Get the logged-in user
            UserEntity user = userService.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Create order
            OrderEntity order = new OrderEntity();
            order.setUser(user);
            order.setStatus(OrderStatus.PENDING);
            order.setShippingAddress(formatShippingAddress(checkoutDto));
            order.setTotalAmount(BigDecimal.ZERO);

            BigDecimal totalAmount = BigDecimal.ZERO;

            // Add order items from cart
            if (checkoutDto.getCartItems() != null) {
                for (CheckoutDto.CartItemDto cartItem : checkoutDto.getCartItems()) {
                    var variant = productVariantService.findOne(cartItem.getVariantId());
                    if (variant != null) {
                        OrderItemEntity orderItem = new OrderItemEntity();
                        orderItem.setProductVariant(
                                productVariantService.findEntityById(cartItem.getVariantId())
                        );
                        orderItem.setQuantity(cartItem.getQuantity());
                        orderItem.setUnitPrice(variant.getPrice());
                        order.addOrderItem(orderItem);

                        totalAmount = totalAmount.add(
                                variant.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity()))
                        );
                    }
                }
            }

            order.setTotalAmount(totalAmount);
            OrderEntity savedOrder = orderRepository.save(order);

            // Create Stripe PaymentIntent
            PaymentIntentDto paymentIntent = paymentService.createPaymentIntent(savedOrder);

            return ResponseEntity.ok(Map.of(
                    "orderId", savedOrder.getId(),
                    "clientSecret", paymentIntent.getClientSecret(),
                    "amount", paymentIntent.getAmount()
            ));

        } catch (Exception e) {
            log.error("Error creating order: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/checkout/confirm")
    @ResponseBody
    public ResponseEntity<?> confirmPayment(@RequestBody Map<String, String> payload) {
        String paymentIntentId = payload.get("paymentIntentId");
        String orderId = payload.get("orderId");

        try {
            boolean success = paymentService.confirmPayment(paymentIntentId);
            if (success) {
                // Update order status
                orderRepository.findById(Long.parseLong(orderId)).ifPresent(order -> {
                    order.setPaymentStatus("paid");
                    order.setStatus(OrderStatus.PAID);
                    orderRepository.save(order);
                });
                return ResponseEntity.ok(Map.of("success", true, "orderId", orderId));
            } else {
                return ResponseEntity.badRequest().body(Map.of("success", false, "error", "Payment not confirmed"));
            }
        } catch (Exception e) {
            log.error("Error confirming payment: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/checkout/success")
    public String checkoutSuccess(@RequestParam(required = false) Long orderId, Model model) {
        if (orderId != null) {
            orderRepository.findById(orderId).ifPresent(order -> {
                model.addAttribute("order", order);
            });
        }
        return "store/checkout-success";
    }

    @PostMapping("/api/stripe/webhook")
    @ResponseBody
    public ResponseEntity<String> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {
        try {
            paymentService.handleWebhookEvent(payload, sigHeader);
            return ResponseEntity.ok("Webhook processed");
        } catch (Exception e) {
            log.error("Webhook error: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Webhook error: " + e.getMessage());
        }
    }

    private String formatShippingAddress(CheckoutDto dto) {
        return String.format("%s %s\n%s\n%s, %s %s\n%s",
                dto.getFirstName(), dto.getLastName(),
                dto.getAddress(),
                dto.getCity(), dto.getState(), dto.getZipCode(),
                dto.getCountry());
    }
}
