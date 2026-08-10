package com.microservices.pro.inventoryservice;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * InventoryService.
 *
 * Session 6 — checkStock(): the synchronous, read-only pre-check called by
 * Order Service via OpenFeign before confirming an order.
 */
@Service
public class InventoryService {

    private final Map<String, StockItem> stock = new ConcurrentHashMap<>(Map.of(
            "PROD-001", new StockItem("PROD-001", 100, 0),
            "PROD-002", new StockItem("PROD-002", 5, 0),
            "PROD-003", new StockItem("PROD-003", 0, 0)  // out of stock
    ));

    private final Map<String, Reservation> reservationsByOrderId = new ConcurrentHashMap<>();

    private record Reservation(String productId, int quantity) {}

    public StockCheckResponse checkStock(String productId, int requestedQty) {
        StockItem item = stock.getOrDefault(productId, new StockItem(productId, 0, 0));
        boolean available = item.hasStock(requestedQty);
        return new StockCheckResponse(productId, requestedQty,
                available, item.availableQuantity() - item.reservedQuantity());
    }


}
