package com.microservices.pro.productservice;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * ProductService — Session 1, Lab 1.
 *
 * In-memory store (no DB yet — see Session 1 homework for the JPA + PostgreSQL
 * upgrade). Implement the 5 TODOs below. See docs/labs/session-01-lab-01.md
 * for the full lab instructions.
 */
@Service
public class ProductService {

    // In-memory storage
    private final Map<Long, Product> products = new ConcurrentHashMap<>();

    // Generates unique IDs
    private final AtomicLong idGenerator = new AtomicLong(1);


    public List<Product> findAll() {
        return new ArrayList<>(products.values());
    }


    public Optional<Product> findById(Long id) {
        return Optional.ofNullable(products.get(id));
    }


    public Product save(Product product) {

        Product productToSave = product;

        if (product.id() == null) {
            productToSave = new Product(
                    idGenerator.getAndIncrement(),
                    product.name(),
                    product.description(),
                    product.price(),
                    product.category()
            );
        }

        products.put(productToSave.id(), productToSave);
        return productToSave;
    }


    public void deleteById(Long id) {
        products.remove(id);
    }
}
