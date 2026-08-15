package com.ecommerce.order.clients;

import io.micrometer.observation.ObservationRegistry;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.propagation.Propagator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@Configuration
public class HttpInterfaceClientConfig {

    @Autowired(required = false)
    private ObservationRegistry observationRegistry;

    @Autowired(required = false)
    private Tracer tracer;

    @Autowired(required = false)
    private Propagator propagator;


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
        RestClient.Builder builder = RestClient.builder();
        if (observationRegistry != null){
            builder.requestInterceptor(createTracingInterceptor());
        }
        return builder;
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

    private ClientHttpRequestInterceptor createTracingInterceptor() {
        return ((request, body, execution) -> {
            if (tracer != null && propagator != null
                    && tracer.currentSpan() != null) {
                propagator.inject(tracer.currentTraceContext().context(),
                        request.getHeaders(),
                        (carrier, key, value) -> carrier.add(key, value));
            }
            return execution.execute(request, body);
        }
        );
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
