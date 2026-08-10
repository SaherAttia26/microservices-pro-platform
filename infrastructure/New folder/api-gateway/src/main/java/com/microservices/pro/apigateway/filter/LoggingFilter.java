package com.microservices.pro.apigateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;

/**
 * LoggingFilter — Session 2, Lab 2A, Task 2.
 *
 * GlobalFilter applied to ALL routes (not route-specific). Implement the
 * TODOs below. See docs/labs/session-02-lab-02.md for the full lab
 * instructions and acceptance criteria.
 */
@Component
public class LoggingFilter implements GlobalFilter, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(LoggingFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        logger.info(
                "Incoming Request: {} {} from {}",
                exchange.getRequest().getMethod(),
                exchange.getRequest().getURI().getPath(),
                exchange.getRequest().getRemoteAddress()
        );

        return chain.filter(exchange)
                .then(Mono.fromRunnable(() -> {
                    HttpStatusCode status = exchange.getResponse().getStatusCode();
                    logger.info("Response Status: {}", status);
                }));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
