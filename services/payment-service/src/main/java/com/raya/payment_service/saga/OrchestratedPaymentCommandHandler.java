package com.raya.payment_service.saga;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.raya.payment_service.service.PaymentException;
import com.raya.payment_service.service.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

/** Session 12 command handler. Payment reports its outcome to the orchestrator only. */
@Service
public class OrchestratedPaymentCommandHandler {

    private static final Logger log = LoggerFactory.getLogger(OrchestratedPaymentCommandHandler.class);
    private final PaymentService paymentService;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public OrchestratedPaymentCommandHandler(PaymentService paymentService,
                                             KafkaTemplate<String, Object> kafkaTemplate,
                                             ObjectMapper objectMapper) {
        this.paymentService = paymentService;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "saga-commands", groupId = "payment-saga-commands")
    public void handle(String rawCommand) {
        try {
            JsonNode command = objectMapper.readTree(rawCommand);
            if (!"PROCESS_PAYMENT".equals(command.path("commandType").asText())) {
                return;
            }
            String orderId = command.path("orderId").asText();
            try {
                String transactionId = paymentService.processPayment(orderId);
                kafkaTemplate.send("saga-results", orderId, new PaymentResult(orderId, true, transactionId, null));
            } catch (PaymentException exception) {
                kafkaTemplate.send("saga-results", orderId,
                        new PaymentResult(orderId, false, null, exception.getMessage()));
            }
        } catch (Exception exception) {
            log.error("[SAGA] Unable to process payment command: {}", rawCommand, exception);
        }
    }

    public record PaymentResult(String orderId, boolean success, String transactionId, String reason) {
        @JsonProperty("resultType")
        public String resultType() {
            return "PAYMENT_RESULT";
        }
    }
}
