package com.microservices.inventoryservice.model;

public record Inventory(
        Long productId,
        Integer quantity
) {
}