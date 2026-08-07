package com.ecommerce.order.clients;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
public class HttpInterfaceClientConfig {
    // 1. Primary plain builder (protects Eureka internal registrations)
    @Bean
    @Primary
    public RestClient.Builder normalRestClientBuilder() {
        return RestClient.builder();
    }

    // 2. Load-balanced builder for Eureka microservice lookup
    @Bean
    @LoadBalanced
    public RestClient.Builder loadBalancedRestClientBuilder() {
        return RestClient.builder();
    }

    private HttpServiceProxyFactory createProxyFactory(RestClient.Builder builder, String baseUrl) {
        RestClient restClient = builder
                .baseUrl(baseUrl)
                .defaultStatusHandler(HttpStatusCode::is4xxClientError, (request, response) -> {
                    System.out.println("Client Error " + response.getStatusCode() + " calling " + baseUrl);
                })
                .build();
        return HttpServiceProxyFactory
                .builderFor(RestClientAdapter.create(restClient))
                .build();
    }

    // =========================================================================
    // 4. Define Client Beans with UNIQUE method names
    // =========================================================================

    @Bean
    public UserServiceClient userServiceClient(@LoadBalanced RestClient.Builder builder) {
        return createProxyFactory(builder, "http://user-service")
                .createClient(UserServiceClient.class);
    }

    @Bean
    public ProductServiceClient productServiceClient(@LoadBalanced RestClient.Builder builder) {
        return createProxyFactory(builder, "http://product-service")
                .createClient(ProductServiceClient.class);
    }
}
