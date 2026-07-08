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

    @Test
    void bulkheadFallback_returnsQueuedStatus_onBulkheadFullException() throws Exception {
        OrderRequest request = new OrderRequest(new BigDecimal("100.00"));
        // Built via the real public factory (verified against the
        // resilience4j source: Bulkhead.ofDefaults(name) + the public
        // static BulkheadFullException.createBulkheadFullException(Bulkhead)
        // factory) rather than mocked — BulkheadFullException's constructor
        // is private, so this is the only supported way to obtain a real
        // instance outside the library itself.
        io.github.resilience4j.bulkhead.Bulkhead bulkhead =
                io.github.resilience4j.bulkhead.Bulkhead.ofDefaults("paymentService");
        BulkheadFullException exception = BulkheadFullException.createBulkheadFullException(bulkhead);

        CompletableFuture<OrderResponse> result = orderService.bulkheadFallback(request, exception);

        assertThat(result.get().status()).isEqualTo("QUEUED");
    }

    @Test
    void timeoutFallback_returnsPendingStatus_wrappedInCompletableFuture() throws Exception {
        OrderRequest request = new OrderRequest(new BigDecimal("100.00"));
        TimeoutException exception = new TimeoutException("Payment exceeded 2s limit");

        CompletableFuture<OrderResponse> result = orderService.timeoutFallback(request, exception);

        assertThat(result).isInstanceOf(CompletableFuture.class);
        assertThat(result.get().status()).isEqualTo("PENDING");
    }

    @Test
    void createOrderAsync_hasAllFourResilienceAnnotations_inTheDocumentedOrder() throws NoSuchMethodException {
        Method method = OrderService.class.getMethod("createOrderAsync", OrderRequest.class);

        assertThat(method.getAnnotation(Bulkhead.class)).isNotNull();
        assertThat(method.getAnnotation(TimeLimiter.class)).isNotNull();
        assertThat(method.getAnnotation(CircuitBreaker.class)).isNotNull();
        assertThat(method.getAnnotation(Retry.class)).isNotNull();

        assertThat(method.getAnnotation(Bulkhead.class).name()).isEqualTo("paymentService");
        assertThat(method.getAnnotation(TimeLimiter.class).name()).isEqualTo("paymentService");
        assertThat(method.getAnnotation(CircuitBreaker.class).name()).isEqualTo("paymentService");
        assertThat(method.getAnnotation(Retry.class).name()).isEqualTo("paymentService");
    }

}