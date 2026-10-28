package com.example.similarproducts.infrastructure;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

import com.example.similarproducts.domain.UpstreamUnavailableException;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = {
        "resilience4j.bulkhead.instances.productDetail.max-concurrent-calls=1",
        "resilience4j.bulkhead.instances.productDetail.max-wait-duration=0"
})
class ProductClientBulkheadTest {

    @RegisterExtension
    static WireMockExtension upstream = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @DynamicPropertySource
    static void upstreamProperties(DynamicPropertyRegistry registry) {
        registry.add("product-api.base-url", upstream::baseUrl);
    }

    @Autowired
    private ProductClient client;

    @Autowired
    private CircuitBreakerRegistry circuitBreakers;

    @Test
    void rejectsCallsBeyondTheConcurrencyLimitWithoutReachingTheProductApi() {
        upstream.stubFor(get(urlMatching("/product/[0-9]+")).willReturn(
                okJson("{\"id\":\"7\",\"name\":\"Slow\",\"price\":1,\"availability\":true}").withFixedDelay(500)));

        List<String> outcomes;
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<CompletableFuture<String>> calls = List.of("7", "8").stream()
                    .map(id -> CompletableFuture.supplyAsync(() -> lookUp(id), executor))
                    .toList();
            outcomes = calls.stream().map(CompletableFuture::join).toList();
        }

        assertThat(outcomes).containsExactlyInAnyOrder("found", "rejected");
        upstream.verify(1, getRequestedFor(urlMatching("/product/[0-9]+")));
        // Our own concurrency limit is not a sign that the product API is unhealthy
        assertThat(circuitBreakers.circuitBreaker("productDetail").getMetrics().getNumberOfFailedCalls()).isZero();
    }

    private String lookUp(String id) {
        try {
            return client.getProductDetail(id).isPresent() ? "found" : "missing";
        } catch (UpstreamUnavailableException e) {
            return "rejected";
        }
    }
}
