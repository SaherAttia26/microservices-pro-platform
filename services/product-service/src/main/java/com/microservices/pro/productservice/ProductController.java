package com.microservices.pro.productservice;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * ProductController — Session 1, Lab 1.
 *
 * Implement the endpoints below. See docs/labs/session-01-lab-01.md for the
 * full lab instructions and acceptance criteria.
 *
 *   GET    /api/v1/products       → all products
 *   GET    /api/v1/products/{id}  → product by id (404 if not found)
 *   POST   /api/v1/products       → create a new product (201 Created)
 *   DELETE /api/v1/products/{id}  → delete a product
 */
@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public List<Product> getAllProducts() {
        return productService.findAll();
    }

    @GetMapping("/{id}")
    public Product getProductById(@PathVariable Long id) {
        return productService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Product createProduct(@RequestBody Product product) {
        return productService.save(product);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProduct(@PathVariable Long id) {
        productService.deleteById(id);
    }

}
