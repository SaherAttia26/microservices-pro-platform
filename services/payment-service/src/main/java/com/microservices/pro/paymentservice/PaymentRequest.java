package com.microservices.pro.paymentservice;

import java.math.BigDecimal;


public record PaymentRequest(BigDecimal amount) {}