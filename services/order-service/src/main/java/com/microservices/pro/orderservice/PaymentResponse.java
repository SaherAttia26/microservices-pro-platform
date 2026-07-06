package com.microservices.pro.orderservice;

import java.math.BigDecimal;
import java.util.UUID;


public record PaymentResponse(String transactionId, BigDecimal amount) {

    public PaymentResponse(BigDecimal amount) {
        this(UUID.randomUUID().toString(), amount);
    }
}