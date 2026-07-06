package com.microservices.pro.orderservice;

import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @InjectMocks
    private OrderService orderService;

    @Test
    void paymentFallback_returnsPendingOrderResponse() throws Exception {
        OrderRequest request = new OrderRequest(new BigDecimal("100.00"));
        Throwable exception = new RuntimeException("Payment Service unavailable");

        CompletableFuture<OrderResponse> result = orderService.paymentFallback(request, exception);

        assertThat(result.get().status()).isEqualTo("PENDING");
    }

    @Test
    void paymentFallback_hasCorrectSignature_requestPlusThrowable() throws NoSuchMethodException {
        Method fallback = OrderService.class.getMethod("paymentFallback", OrderRequest.class, Throwable.class);

        assertThat(fallback.getParameterCount()).isEqualTo(2);
        assertThat(fallback.getParameterTypes()[0]).isEqualTo(OrderRequest.class);
        assertThat(fallback.getParameterTypes()[1]).isEqualTo(Throwable.class);
    }

}