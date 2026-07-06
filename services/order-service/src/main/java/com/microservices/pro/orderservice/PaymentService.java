package com.microservices.pro.orderservice;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Random;
import java.util.UUID;


@Service
public class PaymentService {

    private final Random random = new Random();

    public PaymentResponse processPayment(PaymentRequest request) {
        if (random.nextInt(10) < 5) {  // 50% chance
            throw new RuntimeException("Payment Service unavailable");
        }
        return new PaymentResponse(request.amount());
    }
}