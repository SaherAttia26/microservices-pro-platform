package com.raya.order_service.controller;

import com.raya.order_service.dto.OrderRequest;
import com.raya.order_service.dto.OrderResponse;
import com.raya.order_service.service.OrderService;
import com.raya.order_service.saga.OrderSagaOrchestrator;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * OrderController — Session 7 (Choreography Saga).
 *
 * POST creates the order and starts the saga (returns PENDING immediately).
 * GET returns the current persisted status — which becomes CONFIRMED or
 * CANCELLED once the saga completes via Kafka events.
 */
@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService orderService;
    private final OrderSagaOrchestrator orderSagaOrchestrator;

    public OrderController(OrderService orderService, OrderSagaOrchestrator orderSagaOrchestrator) {
        this.orderService = orderService;
        this.orderSagaOrchestrator = orderSagaOrchestrator;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@RequestBody OrderRequest request) {
        return ResponseEntity.ok(orderService.createOrder(request));
    }

    @PostMapping("/saga")
    public ResponseEntity<OrderResponse> startOrchestratedSaga(@RequestBody OrderRequest request) {
        return ResponseEntity.ok(orderSagaOrchestrator.startSaga(request));
    }

    @GetMapping("/{id}/status")
    public ResponseEntity<String> getStatus(@PathVariable String id) {
        return orderService.findById(id)
                .map(order -> ResponseEntity.ok(order.status().name()))
                .orElse(ResponseEntity.notFound().build());
    }
}
