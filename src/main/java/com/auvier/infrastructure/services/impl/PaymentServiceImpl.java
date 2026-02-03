package com.auvier.infrastructure.services.impl;

import com.auvier.dtos.PaymentIntentDto;
import com.auvier.entities.OrderEntity;
import com.auvier.enums.OrderStatus;
import com.auvier.infrastructure.services.PaymentService;
import com.auvier.repositories.OrderRepository;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.net.Webhook;
import com.stripe.param.PaymentIntentCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
            // Convert amount to cents (Stripe uses smallest currency unit)
            long amountInCents = order.getTotalAmount().multiply(new java.math.BigDecimal(100)).longValue();

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

            // Update order with payment intent ID
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
            log.error("Stripe error creating payment intent for order {}: {}", order.getId(), e.getMessage());
            throw new RuntimeException("Failed to create payment: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean confirmPayment(String paymentIntentId) {
        try {
            PaymentIntent paymentIntent = PaymentIntent.retrieve(paymentIntentId);
            return "succeeded".equals(paymentIntent.getStatus());
        } catch (StripeException e) {
            log.error("Error confirming payment {}: {}", paymentIntentId, e.getMessage());
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
            log.error("Webhook signature verification failed: {}", e.getMessage());
            throw new RuntimeException("Invalid webhook signature");
        }

        log.info("Received Stripe webhook event: {}", event.getType());

        switch (event.getType()) {
            case "payment_intent.succeeded" -> handlePaymentSucceeded(event);
            case "payment_intent.payment_failed" -> handlePaymentFailed(event);
            default -> log.info("Unhandled event type: {}", event.getType());
        }
    }

    private void handlePaymentSucceeded(Event event) {
        PaymentIntent paymentIntent = (PaymentIntent) event.getDataObjectDeserializer()
                .getObject().orElse(null);

        if (paymentIntent == null) {
            log.error("PaymentIntent is null in succeeded event");
            return;
        }

        String orderId = paymentIntent.getMetadata().get("order_id");
        if (orderId != null) {
            orderRepository.findById(Long.parseLong(orderId)).ifPresent(order -> {
                order.setPaymentStatus("paid");
                order.setStatus(OrderStatus.PAID);
                order.setPaymentMethod("stripe");
                orderRepository.save(order);
                log.info("Order {} marked as PAID", orderId);
            });
        }
    }

    private void handlePaymentFailed(Event event) {
        PaymentIntent paymentIntent = (PaymentIntent) event.getDataObjectDeserializer()
                .getObject().orElse(null);

        if (paymentIntent == null) {
            log.error("PaymentIntent is null in failed event");
            return;
        }

        String orderId = paymentIntent.getMetadata().get("order_id");
        if (orderId != null) {
            orderRepository.findById(Long.parseLong(orderId)).ifPresent(order -> {
                order.setPaymentStatus("failed");
                orderRepository.save(order);
                log.info("Order {} payment failed", orderId);
            });
        }
    }
}
