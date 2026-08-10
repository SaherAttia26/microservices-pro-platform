package com.raya.product_service.repository;

import com.raya.product_service.model.Product;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.containers.PostgreSQLContainer;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ProductRepositoryIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("productdb_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @Autowired
    private ProductRepository productRepository;

    @Test
    void saveAndFindById_roundTripsProductAgainstPostgres() {
        Product saved = productRepository.save(new Product("Laptop", "15-inch laptop", new BigDecimal("999.99"), "Electronics"));

        assertThat(productRepository.findById(saved.getId()))
                .isPresent()
                .get()
                .extracting(Product::getName, Product::getPrice)
                .containsExactly("Laptop", new BigDecimal("999.99"));
    }

    @Test
    void findByPriceLessThan_returnsOnlyCheaperProducts() {
        productRepository.save(new Product("Mouse", "Wireless mouse", new BigDecimal("25.00"), "Accessories"));
        productRepository.save(new Product("Laptop", "15-inch laptop", new BigDecimal("999.99"), "Electronics"));

        assertThat(productRepository.findByPriceLessThan(new BigDecimal("100.00")))
                .extracting(Product::getName)
                .containsExactly("Mouse");
    }
}
