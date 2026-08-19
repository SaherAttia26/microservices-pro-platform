package com.raya.order_service.saga;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.raya.order_service.dto.OrderRequest;
import com.raya.order_service.dto.OrderResponse;
import com.raya.order_service.model.Order;
import com.raya.order_service.model.OrderStatus;
import com.raya.order_service.repository.OrderRepository;
import com.raya.order_service.saga.command.ProcessPaymentCommand;
import com.raya.order_service.saga.command.ReleaseInventoryCommand;
import com.raya.order_service.saga.command.ReserveInventoryCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Session 12 orchestration flow. The existing Session 7 choreography flow remains
 * available through {@code OrderService#createOrder}.
 *
 * <p>DEV ONLY: state is intentionally in-memory for this lab and is lost on restart.</p>
 */
@Service
public class OrderSagaOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(OrderSagaOrchestrator.class);
    private final Map<String, SagaState> sagaStates = new ConcurrentHashMap<>();
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final OrderRepository orderRepository;
    private final ObjectMapper objectMapper;

    public OrderSagaOrchestrator(KafkaTemplate<String, Object> kafkaTemplate,
                                 OrderRepository orderRepository,
                                 ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.orderRepository = orderRepository;
        this.objectMapper = objectMapper;
    }

    public OrderResponse startSaga(OrderRequest request) {
        String orderId = UUID.randomUUID().toString();
        Order order = new Order(orderId, request.productId(), request.quantity(), request.amount(),
                OrderStatus.PENDING, request.customerId());
        orderRepository.save(order);

        sagaStates.put(orderId, SagaState.STARTED);
        kafkaTemplate.send("saga-commands", orderId,
                new ReserveInventoryCommand(orderId, request.productId(), request.quantity()));
        transition(orderId, SagaState.INVENTORY_RESERVING);
        return new OrderResponse(orderId, OrderStatus.PENDING.name(), "Order received - processing...");
    }

    /** Results are routed by their explicit resultType property to keep Kafka payloads interoperable. */
    @KafkaListener(topics = "saga-results", groupId = "order-saga-orchestrator")
    public void handleSagaResult(String rawEvent) {
        try {
            JsonNode event = objectMapper.readTree(rawEvent);
            String resultType = event.path("resultType").asText();
            String orderId = event.path("orderId").asText();
            switch (resultType) {
                case "INVENTORY_RESULT" -> handleInventoryResult(orderId, event.path("success").asBoolean());
                case "PAYMENT_RESULT" -> handlePaymentResult(orderId, event.path("success").asBoolean());
                case "INVENTORY_RELEASED" -> handleInventoryReleased(orderId);
                default -> log.debug("[SAGA] Ignoring unknown saga result: {}", resultType);
            }
        } catch (Exception exception) {
            log.error("[SAGA] Unable to process saga result: {}", rawEvent, exception);
        }
    }

    void handleInventoryResult(String orderId, boolean success) {
        if (sagaStates.get(orderId) != SagaState.INVENTORY_RESERVING) {
            log.warn("[SAGA] Unexpected inventory result for order {}", orderId);
            return;
        }
        if (success) {
            transition(orderId, SagaState.INVENTORY_RESERVED);
            Order order = orderRepository.findById(orderId).orElseThrow();
            kafkaTemplate.send("saga-commands", orderId, new ProcessPaymentCommand(orderId, order.amount()));
            transition(orderId, SagaState.PAYMENT_PROCESSING);
        } else {
            transition(orderId, SagaState.INVENTORY_RESERVE_FAILED);
            finish(orderId, OrderStatus.CANCELLED);
        }
    }

    void handlePaymentResult(String orderId, boolean success) {
        if (sagaStates.get(orderId) != SagaState.PAYMENT_PROCESSING) {
            log.warn("[SAGA] Unexpected payment result for order {}", orderId);
            return;
        }
        if (success) {
            transition(orderId, SagaState.COMPLETED);
            finish(orderId, OrderStatus.CONFIRMED);
        } else {
            transition(orderId, SagaState.PAYMENT_FAILED);
            kafkaTemplate.send("saga-commands", orderId, new ReleaseInventoryCommand(orderId));
            transition(orderId, SagaState.INVENTORY_RELEASING);
        }
    }

    void handleInventoryReleased(String orderId) {
        if (sagaStates.get(orderId) != SagaState.INVENTORY_RELEASING) {
            log.warn("[SAGA] Unexpected inventory release for order {}", orderId);
            return;
        }
        transition(orderId, SagaState.CANCELLED);
        finish(orderId, OrderStatus.CANCELLED);
    }

    SagaState stateFor(String orderId) {
        return sagaStates.get(orderId);
    }

    private void finish(String orderId, OrderStatus status) {
        Order order = orderRepository.findById(orderId).orElseThrow();
        order.setStatus(status);
        orderRepository.save(order);
        sagaStates.remove(orderId);
        log.info("[SAGA] Order {} {}", orderId, status);
    }

    private void transition(String orderId, SagaState newState) {
        SagaState oldState = sagaStates.put(orderId, newState);
        log.info("[SAGA] {} {} -> {}", orderId, oldState, newState);
    }
}
