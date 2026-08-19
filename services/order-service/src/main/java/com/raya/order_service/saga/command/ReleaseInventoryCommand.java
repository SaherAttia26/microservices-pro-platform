package com.raya.order_service.saga.command;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ReleaseInventoryCommand(String orderId) {
    @JsonProperty("commandType")
    public String commandType() {
        return "RELEASE_INVENTORY";
    }
}
