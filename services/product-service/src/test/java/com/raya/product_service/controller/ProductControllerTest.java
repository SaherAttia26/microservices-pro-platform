package com.raya.product_service.controller;

import com.raya.product_service.model.Product;
import com.raya.product_service.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductController.class)
@Import(ProductControllerTest.CacheConfiguration.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductService productService;

    @Test
    void findById_returnsOk_whenProductExists() throws Exception {
        when(productService.findById(1L)).thenReturn(Optional.of(product(1L)));

        mockMvc.perform(get("/api/v1/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Laptop"));
    }

    @Test
    void findById_returnsNotFound_whenProductDoesNotExist() throws Exception {
        when(productService.findById(404L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/products/404"))
                .andExpect(status().isNotFound());
    }

    @Test
    void create_returnsCreated_withSavedProduct() throws Exception {
        when(productService.save(any(Product.class))).thenReturn(product(1L));

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Laptop","description":"15-inch laptop","price":999.99,"category":"Electronics"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.price").value(999.99));
    }

    @Test
    void create_returnsBadRequest_whenNameIsMissing() throws Exception {
        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description":"15-inch laptop","price":999.99,"category":"Electronics"}
                                """))
                .andExpect(status().isBadRequest());
    }

    private Product product(Long id) {
        return new Product(id, "Laptop", "15-inch laptop", new BigDecimal("999.99"), "Electronics");
    }

    @TestConfiguration
    static class CacheConfiguration {
        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager("products");
        }
    }
}
