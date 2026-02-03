package com.auvier.infrastructure.services;

import com.auvier.dtos.PaymentIntentDto;
import com.auvier.entities.OrderEntity;

public interface PaymentService {

    /**
     * Create a Stripe Payment Intent for an order
     */
    PaymentIntentDto createPaymentIntent(OrderEntity order);

    /**
     * Confirm payment was successful
     */
    boolean confirmPayment(String paymentIntentId);

    /**
     * Handle webhook events from Stripe
     */
    void handleWebhookEvent(String payload, String sigHeader);
}
