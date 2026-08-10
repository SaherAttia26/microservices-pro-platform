package com.microservices.inventoryservice.service;

import com.microservices.inventoryservice.model.Inventory;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class InventoryService {

    private final Map<Long, Inventory> inventory = Map.of(
            1L, new Inventory(1L, 15),
            2L, new Inventory(2L, 7),
            3L, new Inventory(3L, 0)
    );

    public Inventory getInventory(Long productId) {
        return inventory.get(productId);
    }

}