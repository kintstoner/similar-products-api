package com.example.similarproducts.infrastructure;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.similarproducts.domain.UpstreamUnavailableException;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class ProductClientCacheTest {

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
    private CacheManager cacheManager;

    @BeforeEach
    void clearCaches() {
        cacheManager.getCacheNames().forEach(name -> cacheManager.getCache(name).clear());
    }

    @Test
    void cachesProductDetails() {
        upstream.stubFor(get("/product/1").willReturn(
                okJson("{\"id\":\"1\",\"name\":\"Shirt\",\"price\":9.99,\"availability\":true}")));

        client.getProductDetail("1");
        assertThat(client.getProductDetail("1")).isPresent();

        upstream.verify(1, getRequestedFor(urlEqualTo("/product/1")));
    }

    @Test
    void cachesProductsThatAreNotFound() {
        upstream.stubFor(get("/product/5").willReturn(aResponse().withStatus(404)));

        client.getProductDetail("5");
        assertThat(client.getProductDetail("5")).isEmpty();

        upstream.verify(1, getRequestedFor(urlEqualTo("/product/5")));
    }

    @Test
    void doesNotCacheFailures() {
        upstream.stubFor(get("/product/6").willReturn(aResponse().withStatus(500)));

        assertThatThrownBy(() -> client.getProductDetail("6")).isInstanceOf(UpstreamUnavailableException.class);
        assertThatThrownBy(() -> client.getProductDetail("6")).isInstanceOf(UpstreamUnavailableException.class);

        upstream.verify(2, getRequestedFor(urlEqualTo("/product/6")));
    }

    @Test
    void cachesSimilarIds() {
        upstream.stubFor(get("/product/1/similarids").willReturn(okJson("[2,3,4]")));

        client.getSimilarIds("1");
        assertThat(client.getSimilarIds("1")).containsExactly("2", "3", "4");

        upstream.verify(1, getRequestedFor(urlEqualTo("/product/1/similarids")));
    }
}
