package com.microservices.inventoryservice.controller;

import com.microservices.inventoryservice.model.Inventory;
import com.microservices.inventoryservice.service.InventoryService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/inventory")
public class InventoryController {

    private final InventoryService service;

    public InventoryController(InventoryService service) {
        this.service = service;
    }

    @GetMapping("/{productId}")
    public Inventory getInventory(@PathVariable Long productId) {
        return service.getInventory(productId);
    }

}