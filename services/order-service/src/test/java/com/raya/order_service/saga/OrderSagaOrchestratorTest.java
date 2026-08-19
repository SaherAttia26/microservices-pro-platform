package com.raya.order_service.saga;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.raya.order_service.dto.OrderRequest;
import com.raya.order_service.model.Order;
import com.raya.order_service.repository.OrderRepository;
import com.raya.order_service.saga.command.ReleaseInventoryCommand;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderSagaOrchestratorTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private OrderRepository orderRepository;

    @Test
    void startSaga_reservesInventoryAndTracksPendingState() {
        OrderSagaOrchestrator orchestrator = new OrderSagaOrchestrator(kafkaTemplate, orderRepository, new ObjectMapper());
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = orchestrator.startSaga(new OrderRequest("PROD-001", 2, BigDecimal.TEN, "customer-1"));

        assertThat(response.status()).isEqualTo("PENDING");
        assertThat(orchestrator.stateFor(response.orderId())).isEqualTo(SagaState.INVENTORY_RESERVING);
        verify(kafkaTemplate).send(eq("saga-commands"), eq(response.orderId()), any());
    }

    @Test
    void failedPayment_sendsReleaseInventoryCompensation() {
        OrderSagaOrchestrator orchestrator = new OrderSagaOrchestrator(kafkaTemplate, orderRepository, new ObjectMapper());
        Order order = new Order("ignored", "PROD-001", 2, BigDecimal.TEN,
                com.raya.order_service.model.OrderStatus.PENDING, "customer-1");
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderRepository.findById(any())).thenReturn(Optional.of(order));

        var response = orchestrator.startSaga(new OrderRequest("PROD-001", 2, BigDecimal.TEN, "customer-1"));
        orchestrator.handleInventoryResult(response.orderId(), true);
        orchestrator.handlePaymentResult(response.orderId(), false);

        ArgumentCaptor<Object> command = ArgumentCaptor.forClass(Object.class);
        verify(kafkaTemplate, org.mockito.Mockito.times(3))
                .send(eq("saga-commands"), eq(response.orderId()), command.capture());
        assertThat(command.getAllValues()).anyMatch(ReleaseInventoryCommand.class::isInstance);
        assertThat(orchestrator.stateFor(response.orderId())).isEqualTo(SagaState.INVENTORY_RELEASING);
    }
}
