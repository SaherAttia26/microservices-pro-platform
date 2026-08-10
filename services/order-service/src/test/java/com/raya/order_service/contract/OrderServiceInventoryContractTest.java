package com.raya.order_service.contract;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.LambdaDsl;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.PactSpecVersion;
import au.com.dius.pact.core.model.annotations.Pact;
import com.raya.order_service.dto.StockCheckResponse;
import com.raya.order_service.service.InventoryClient;
import feign.Feign;
import feign.jackson.JacksonDecoder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = "inventory-service", pactVersion = PactSpecVersion.V3)
class OrderServiceInventoryContractTest {

    @Pact(consumer = "order-service", provider = "inventory-service")
    RequestResponsePact checkStockAvailable(PactDslWithProvider builder) {
        return builder
                .given("PROD-001 has 100 units in stock")
                .uponReceiving("a stock check for PROD-001 quantity 5")
                .path("/api/v1/inventory/check")
                .method("GET")
                .query("productId=PROD-001&quantity=5")
                .willRespondWith()
                .status(200)
                .body(LambdaDsl.newJsonBody(body -> body
                        .stringType("productId", "PROD-001")
                        .integerType("requestedQuantity", 5)
                        .booleanValue("available", true)
                        .integerType("remainingStock", 95)
                ).build())
                .toPact();
    }

    @Test
    @PactTestFor(pactMethod = "checkStockAvailable")
    void checkStock_deserializesTheInventoryContract(MockServer mockServer) {
        InventoryClient client = Feign.builder()
                .decoder(new JacksonDecoder())
                .target(InventoryClient.class, mockServer.getUrl());

        StockCheckResponse response = client.checkStock("PROD-001", 5);

        assertThat(response.available()).isTrue();
        assertThat(response.remainingStock()).isPositive();
    }
}
