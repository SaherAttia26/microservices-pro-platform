package com.raya.inventory_service.saga;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.raya.inventory_service.service.InsufficientStockException;
import com.raya.inventory_service.service.InventoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

/** Session 12 command handler. It does not decide the next saga step. */
@Service
public class OrchestratedInventoryCommandHandler {

    private static final Logger log = LoggerFactory.getLogger(OrchestratedInventoryCommandHandler.class);
    private final InventoryService inventoryService;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public OrchestratedInventoryCommandHandler(InventoryService inventoryService,
                                               KafkaTemplate<String, Object> kafkaTemplate,
                                               ObjectMapper objectMapper) {
        this.inventoryService = inventoryService;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "saga-commands", groupId = "inventory-saga-commands")
    public void handle(String rawCommand) {
        try {
            JsonNode command = objectMapper.readTree(rawCommand);
            String type = command.path("commandType").asText();
            String orderId = command.path("orderId").asText();
            if ("RESERVE_INVENTORY".equals(type)) {
                reserve(orderId, command.path("productId").asText(), command.path("quantity").asInt());
            } else if ("RELEASE_INVENTORY".equals(type)) {
                inventoryService.releaseStock(orderId);
                kafkaTemplate.send("saga-results", orderId, new InventoryReleasedResult(orderId));
            }
        } catch (Exception exception) {
            log.error("[SAGA] Unable to process inventory command: {}", rawCommand, exception);
        }
    }

    private void reserve(String orderId, String productId, int quantity) {
        try {
            inventoryService.reserveStock(productId, quantity, orderId);
            kafkaTemplate.send("saga-results", orderId, new InventoryResult(orderId, true, null));
        } catch (InsufficientStockException exception) {
            kafkaTemplate.send("saga-results", orderId,
                    new InventoryResult(orderId, false, exception.getMessage()));
        }
    }

    public record InventoryResult(String orderId, boolean success, String reason) {
        @JsonProperty("resultType")
        public String resultType() {
            return "INVENTORY_RESULT";
        }
    }

    public record InventoryReleasedResult(String orderId) {
        @JsonProperty("resultType")
        public String resultType() {
            return "INVENTORY_RELEASED";
        }
    }
}
