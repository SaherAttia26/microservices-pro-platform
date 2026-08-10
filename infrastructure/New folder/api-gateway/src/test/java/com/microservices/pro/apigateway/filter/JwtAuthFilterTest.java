package com.microservices.pro.apigateway.filter;

import com.microservices.pro.apigateway.security.JwtUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.Ordered;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthFilterTest {

    @InjectMocks
    private JwtAuthFilter jwtAuthFilter;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private GatewayFilterChain filterChain;

    @Test
    void getOrder_returnsHighestPrecedencePlusOne() {
        assertThat(jwtAuthFilter.getOrder())
                .isEqualTo(Ordered.HIGHEST_PRECEDENCE + 1);
    }

    @Test
    void filter_callsChainFilter_forPublicRoutes() {
        MockServerHttpRequest request = MockServerHttpRequest
                .get("/api/v1/products")
                .build();

        ServerWebExchange exchange = MockServerWebExchange.from(request);

        when(filterChain.filter(exchange)).thenReturn(Mono.empty());

        StepVerifier.create(jwtAuthFilter.filter(exchange, filterChain))
                .verifyComplete();

        verify(filterChain).filter(exchange);
        verifyNoInteractions(jwtUtil);
    }
}