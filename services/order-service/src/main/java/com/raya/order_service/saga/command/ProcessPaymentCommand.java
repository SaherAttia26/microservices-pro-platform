package com.raya.order_service.saga.command;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

public record ProcessPaymentCommand(String orderId, BigDecimal amount) {
    @JsonProperty("commandType")
    public String commandType() {
        return "PROCESS_PAYMENT";
    }
}
