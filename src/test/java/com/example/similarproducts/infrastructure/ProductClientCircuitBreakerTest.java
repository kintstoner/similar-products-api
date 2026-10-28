package com.example.similarproducts.infrastructure;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.similarproducts.domain.UpstreamUnavailableException;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = {
        "resilience4j.circuitbreaker.instances.productDetail.sliding-window-size=4",
        "resilience4j.circuitbreaker.instances.productDetail.minimum-number-of-calls=4",
        "resilience4j.circuitbreaker.instances.productDetail.failure-rate-threshold=50",
        "resilience4j.circuitbreaker.instances.productDetail.wait-duration-in-open-state=1h"
})
@ExtendWith(OutputCaptureExtension.class)
class ProductClientCircuitBreakerTest {

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

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void resetState() {
        circuitBreakers.getAllCircuitBreakers().forEach(CircuitBreaker::reset);
        cacheManager.getCacheNames().forEach(name -> cacheManager.getCache(name).clear());
    }

    @Test
    void stopsCallingTheProductApiOnceItKeepsFailing() {
        upstream.stubFor(get("/product/6").willReturn(aResponse().withStatus(500)));

        for (int i = 0; i < 6; i++) {
            assertThatThrownBy(() -> client.getProductDetail("6")).isInstanceOf(UpstreamUnavailableException.class);
        }

        upstream.verify(4, getRequestedFor(urlEqualTo("/product/6")));
    }

    @Test
    void logsWhenTheCircuitOpens(CapturedOutput output) {
        upstream.stubFor(get("/product/6").willReturn(aResponse().withStatus(500)));

        for (int i = 0; i < 4; i++) {
            assertThatThrownBy(() -> client.getProductDetail("6")).isInstanceOf(UpstreamUnavailableException.class);
        }

        assertThat(output).contains("Circuit breaker productDetail changed from CLOSED to OPEN");
    }

    @Test
    void notFoundProductsDoNotOpenTheCircuit() {
        upstream.stubFor(get(urlMatching("/product/[0-9]+")).willReturn(aResponse().withStatus(404)));

        for (int id = 10; id < 16; id++) {
            assertThat(client.getProductDetail(String.valueOf(id))).isEmpty();
        }

        assertThat(circuitBreakers.circuitBreaker("productDetail").getState()).isEqualTo(CircuitBreaker.State.CLOSED);
    }
}
