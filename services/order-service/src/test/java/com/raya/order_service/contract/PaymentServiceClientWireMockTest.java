package com.raya.order_service.contract;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.raya.order_service.dto.PaymentRequest;
import com.raya.order_service.dto.PaymentResponse;
import com.raya.order_service.service.PaymentServiceClient;
import feign.Feign;
import feign.FeignException;
import feign.jackson.JacksonDecoder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentServiceClientWireMockTest {

    private WireMockServer wireMockServer;
    private PaymentServiceClient paymentClient;

    @BeforeEach
    void startServer() {
        wireMockServer = new WireMockServer();
        wireMockServer.start();
        paymentClient = Feign.builder()
                .decoder(new JacksonDecoder())
                .target(PaymentServiceClient.class, wireMockServer.baseUrl());
    }

    @AfterEach
    void stopServer() {
        wireMockServer.stop();
    }

    @Test
    void processPayment_deserializesApprovedResponse() {
        stubFor(post(urlEqualTo("/api/v1/payments"))
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"transactionId\":\"TXN-001\",\"status\":\"APPROVED\",\"amount\":100.00}")));

        PaymentResponse response = paymentClient.processPayment(new PaymentRequest(new BigDecimal("100.00")));

        assertThat(response.transactionId()).isEqualTo("TXN-001");
        assertThat(response.status()).isEqualTo("APPROVED");
    }

    @Test
    void processPayment_propagatesServiceUnavailableResponse() {
        stubFor(post(urlEqualTo("/api/v1/payments"))
                .willReturn(aResponse().withStatus(503)));

        assertThatThrownBy(() -> paymentClient.processPayment(new PaymentRequest(new BigDecimal("100.00"))))
                .isInstanceOf(FeignException.ServiceUnavailable.class);
    }

    @Test
    void processPayment_sendsExpectedAmount() {
        stubFor(post(urlEqualTo("/api/v1/payments"))
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"transactionId\":\"TXN-002\",\"status\":\"APPROVED\",\"amount\":250.00}")));

        paymentClient.processPayment(new PaymentRequest(new BigDecimal("250.00")));

        verify(postRequestedFor(urlEqualTo("/api/v1/payments"))
                .withRequestBody(equalToJson("{\"amount\":250.00}")));
    }
}
