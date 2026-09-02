package com.gateway;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Optional;

@Configuration
public class GatewayConfig {

    private record RouteDefinition(
            String id,
            String uri,
            String[] paths,
            String rewriteRegex,
            String rewriteReplacement,
            String setPath,
            String circuitBreakerName,
            String fallbackUri,
            boolean enableRateLimiter
    ) {
        // Constructor with rewrite + circuit breaker + rate limiter
        public RouteDefinition(String id, String uri, String[] paths, String rewriteRegex, String rewriteReplacement, String circuitBreakerName, String fallbackUri, boolean enableRateLimiter) {
            this(id, uri, paths, rewriteRegex, rewriteReplacement, null, circuitBreakerName, fallbackUri, enableRateLimiter);
        }

        // Constructor with rewrite + circuit breaker
        public RouteDefinition(String id, String uri, String[] paths, String rewriteRegex, String rewriteReplacement, String circuitBreakerName, String fallbackUri) {
            this(id, uri, paths, rewriteRegex, rewriteReplacement, null, circuitBreakerName, fallbackUri, false);
        }

        // Constructor with rewrite only
        public RouteDefinition(String id, String uri, String[] paths, String rewriteRegex, String rewriteReplacement) {
            this(id, uri, paths, rewriteRegex, rewriteReplacement, null, null, null, false);
        }

        // Constructor for setPath only
        public RouteDefinition(String id, String uri, String[] paths, String setPath) {
            this(id, uri, paths, null, null, setPath, null, null, false);
        }

        // Constructor for simple routes without filters
        public RouteDefinition(String id, String uri, String[] paths) {
            this(id, uri, paths, null, null, null, null, null, false);
        }
    }

    private static final List<RouteDefinition> ROUTES = List.of(
            new RouteDefinition(
                    "product-service",
                    "lb://PRODUCT-SERVICE",
                    new String[]{"/products/**", "/api/products/**"},
                    "/(api/)?products(?<segment>/?.*)",
                    "/api/products${segment}",
                    null,
                    "productServiceCircuitBreaker",
                    "forward:/fallback/product",
                    true
            ),
            new RouteDefinition(
                    "user-service",
                    "lb://USER-SERVICE",
                    new String[]{"/users/**", "/api/users/**"},
                    "/(api/)?users(?<segment>/?.*)",
                    "/api/users${segment}"
            ),
            // Circuit Breaker + Rate Limiter configured on Order Service
            new RouteDefinition(
                    "order-service",
                    "lb://ORDER-SERVICE",
                    new String[]{"/orders/**", "/api/orders/**", "/cart/**", "/api/cart/**"},
                    "/(api/)?(?<segment>.*)",
                    "/api/${segment}",
                    "ecomBreaker",
                    "forward:/fallback/order",
                    true // 👈 Rate limiting enabled for Order Service
            ),
            new RouteDefinition(
                    "eureka-server-ui",
                    "http://localhost:8762",
                    new String[]{"/eureka/main"},
                    "/"
            ),
            new RouteDefinition(
                    "eureka-server-static",
                    "http://localhost:8762",
                    new String[]{"/eureka/css/**", "/eureka/js/**", "/eureka/fonts/**", "/eureka/images/**"}
            )
    );

    @Bean
    public RedisRateLimiter redisRateLimiter() {
        // replenishRate: 1 token/sec, burstCapacity: 1 token, requestedTokens: 1
        return new RedisRateLimiter(10, 20, 1);
    }

    @Bean
    @Primary
    public KeyResolver hostNameKeyResolver() {
        return exchange -> Mono.just(
                Optional.ofNullable(exchange.getRequest().getRemoteAddress())
                        .map(InetSocketAddress::getHostName)
                        .orElse("anonymous")
        );
    }

    @Bean
    public RouteLocator customRouteLocator(
            RouteLocatorBuilder builder,
            RedisRateLimiter redisRateLimiter,
            KeyResolver keyResolver
    ) {
        RouteLocatorBuilder.Builder routesBuilder = builder.routes();

        ROUTES.forEach(route -> routesBuilder.route(route.id(), r -> {
            var predicateSpec = r.path(route.paths());

            return predicateSpec.filters(f -> {
                // 1. Path Rewriting / Setting
                if (route.setPath() != null) {
                    f.setPath(route.setPath());
                } else if (route.rewriteRegex() != null && route.rewriteReplacement() != null) {
                    f.rewritePath(route.rewriteRegex(), route.rewriteReplacement());
                }

                // 2. Gateway-Level Circuit Breaker
                if (route.circuitBreakerName() != null) {
                    f.circuitBreaker(config -> {
                        config.setName(route.circuitBreakerName());
                        if (route.fallbackUri() != null) {
                            config.setFallbackUri(route.fallbackUri());
                        }
                    });
                }

                // 3. Gateway-Level Request Rate Limiter
                if (route.enableRateLimiter()) {
                    f.requestRateLimiter(config -> config
                            .setRateLimiter(redisRateLimiter)
                            .setKeyResolver(keyResolver)
                    );
                }

                return f;
            }).uri(route.uri());
        }));

        return routesBuilder.build();
    }
}