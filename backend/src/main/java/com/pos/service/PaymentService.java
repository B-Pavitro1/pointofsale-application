package com.pos.service;

import com.pos.exception.BadRequestException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@Slf4j
public class PaymentService {

    /**
     * Simulates payment authorization without storing or accepting PAN, CVV, or card credentials.
     * Compliant with PCI-DSS zero-card-storage requirements.
     */
    public String processPayment(String paymentMethod, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Payment amount must be greater than zero");
        }

        String normalizedMethod = paymentMethod.trim().toUpperCase();
        log.info("Processing {} payment of amount {}", normalizedMethod, amount);

        switch (normalizedMethod) {
            case "CASH":
                return "CASH-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            case "CARD":
                // Simulates external EMV/POS terminal authorization token
                return "AUTH-CARD-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();
            case "MOCK_GATEWAY":
                return "MOCK-GW-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();
            default:
                throw new BadRequestException("Unsupported payment method: " + paymentMethod + ". Allowed: CASH, CARD, MOCK_GATEWAY");
        }
    }
}
