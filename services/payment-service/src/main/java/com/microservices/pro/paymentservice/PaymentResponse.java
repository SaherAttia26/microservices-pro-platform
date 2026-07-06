package com.microservices.pro.paymentservice;

import java.math.BigDecimal;


public record PaymentResponse(String transactionId, String status, BigDecimal amount) {}