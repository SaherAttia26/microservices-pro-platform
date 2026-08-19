package com.raya.order_service.saga.command;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ReserveInventoryCommand(String orderId, String productId, int quantity) {
    @JsonProperty("commandType")
    public String commandType() {
        return "RESERVE_INVENTORY";
    }
}
