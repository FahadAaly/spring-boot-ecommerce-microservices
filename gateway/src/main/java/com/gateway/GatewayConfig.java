package com.gateway;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class GatewayConfig {

    private record RouteDefinition(
            String id,
            String uri,
            String[] paths,
            String rewriteRegex,
            String rewriteReplacement,
            String setPath           // 👈 Optional SetPath parameter
    ) {
        // Overload constructor for standard rewrite routes
        public RouteDefinition(String id, String uri, String[] paths, String rewriteRegex, String rewriteReplacement) {
            this(id, uri, paths, rewriteRegex, rewriteReplacement, null);
        }

        // Overload constructor for simple routes without filters
        public RouteDefinition(String id, String uri, String[] paths) {
            this(id, uri, paths, null, null, null);
        }
    }

    private static final List<RouteDefinition> ROUTES = List.of(
            new RouteDefinition(
                    "product-service",
                    "lb://PRODUCT-SERVICE",
                    new String[]{"/products/**", "/api/products/**"},
                    "/(api/)?products(?<segment>/?.*)",
                    "/api/products${segment}"
            ),
            new RouteDefinition(
                    "user-service",
                    "lb://USER-SERVICE",
                    new String[]{"/users/**", "/api/users/**"},
                    "/(api/)?users(?<segment>/?.*)",
                    "/api/users${segment}"
            ),
            new RouteDefinition(
                    "order-service",
                    "lb://ORDER-SERVICE",
                    new String[]{"/orders/**", "/api/orders/**", "/cart/**", "/api/cart/**"},
                    "/(api/)?(?<segment>.*)",
                    "/api/${segment}"
            ),
            // 👈 Eureka Dashboard HTML using setPath = "/"
            new RouteDefinition(
                    "eureka-server-ui",
                    "http://localhost:8762",
                    new String[]{"/eureka/main"},
                    null,
                    null,
                    "/"
            ),
            // 👈 Eureka Static Resources (No filter)
            new RouteDefinition(
                    "eureka-server-static",
                    "http://localhost:8762",
                    new String[]{"/eureka/css/**", "/eureka/js/**", "/eureka/fonts/**", "/eureka/images/**"}
            )
    );

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        RouteLocatorBuilder.Builder routesBuilder = builder.routes();

        ROUTES.forEach(route -> routesBuilder.route(route.id(), r -> {
            var predicateSpec = r.path(route.paths());

            // 1. Check for setPath filter
            if (route.setPath() != null) {
                return predicateSpec
                        .filters(f -> f.setPath(route.setPath()))
                        .uri(route.uri());
            }

            // 2. Check for rewritePath filter
            if (route.rewriteRegex() != null && route.rewriteReplacement() != null) {
                return predicateSpec
                        .filters(f -> f.rewritePath(route.rewriteRegex(), route.rewriteReplacement()))
                        .uri(route.uri());
            }

            // 3. No filter, forward directly
            return predicateSpec.uri(route.uri());
        }));

        return routesBuilder.build();
    }
}