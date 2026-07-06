package com.microservices.pro.orderservice;

import java.math.BigDecimal;


public record OrderRequest(BigDecimal amount) {}