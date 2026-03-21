package com.devops.orderservice.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    @Value("${payment.mode:mock}")
    private String paymentMode;

    @Value("${payment.real.gateway-url:}")
    private String gatewayUrl;

    /**
     * Process payment — returns a map with "paymentId" and "status".
     * In mock mode, always succeeds with a simulated delay.
     * In real mode, would call the configured gateway URL.
     */
    public Map<String, String> processPayment(Double amount, Long userId) {
        log.info("Processing payment — mode: {}, amount: {}, userId: {}", paymentMode, amount, userId);

        if ("mock".equalsIgnoreCase(paymentMode)) {
            return processMockPayment(amount, userId);
        } else {
            return processRealPayment(amount, userId);
        }
    }

    private Map<String, String> processMockPayment(Double amount, Long userId) {
        log.info("MOCK PAYMENT — simulating payment processing...");

        // Simulate network delay
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        String paymentId = "MOCK-" + UUID.randomUUID().toString();
        log.info("MOCK PAYMENT SUCCESS — paymentId: {}, amount: {}", paymentId, amount);

        return Map.of(
                "paymentId", paymentId,
                "status", "SUCCESS"
        );
    }

    private Map<String, String> processRealPayment(Double amount, Long userId) {
        log.info("REAL PAYMENT — calling gateway at: {}", gatewayUrl);

        // Placeholder for real payment gateway integration
        // In production, this would call Razorpay/Stripe/etc.
        throw new RuntimeException("Real payment gateway not yet configured. " +
                "Set payment.mode=mock in application.yml or configure the gateway.");
    }
}
