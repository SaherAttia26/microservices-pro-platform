package com.microservices.pro.orderservice;

import java.math.BigDecimal;


public record PaymentRequest(BigDecimal amount) {}