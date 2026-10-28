package com.example.similarproducts.infrastructure;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.similarproducts.domain.ProductDetail;
import com.example.similarproducts.domain.ProductNotFoundException;
import com.example.similarproducts.domain.UpstreamUnavailableException;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class ProductClientTest {

    @RegisterExtension
    static WireMockExtension upstream = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @DynamicPropertySource
    static void upstreamProperties(DynamicPropertyRegistry registry) {
        registry.add("product-api.base-url", upstream::baseUrl);
        registry.add("product-api.read-timeout", () -> "300ms");
    }

    @Autowired
    private ProductClient client;

    @Test
    void returnsSimilarIdsInOrderEvenWhenUpstreamSendsNumbers() {
        upstream.stubFor(get("/product/1/similarids").willReturn(okJson("[2,3,4]")));

        assertThat(client.getSimilarIds("1")).containsExactly("2", "3", "4");
    }

    @Test
    void throwsProductNotFoundWhenSimilarIdsReturns404() {
        upstream.stubFor(get("/product/99/similarids").willReturn(aResponse().withStatus(404)));

        assertThatThrownBy(() -> client.getSimilarIds("99"))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void throwsUpstreamUnavailableWhenSimilarIdsReturns500() {
        upstream.stubFor(get("/product/1/similarids").willReturn(aResponse().withStatus(500)));

        assertThatThrownBy(() -> client.getSimilarIds("1")).isInstanceOf(UpstreamUnavailableException.class);
    }

    @Test
    void returnsProductDetail() {
        upstream.stubFor(get("/product/1").willReturn(
                okJson("{\"id\":\"1\",\"name\":\"Shirt\",\"price\":9.99,\"availability\":true}")));

        assertThat(client.getProductDetail("1"))
                .isEqualTo(new ProductDetail("1", "Shirt", new BigDecimal("9.99"), true));
    }

    @Test
    void throwsProductNotFoundWhenDetailReturns404() {
        upstream.stubFor(get("/product/5").willReturn(aResponse().withStatus(404)));

        assertThatThrownBy(() -> client.getProductDetail("5")).isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void throwsUpstreamUnavailableWhenDetailReturns500() {
        upstream.stubFor(get("/product/6").willReturn(aResponse().withStatus(500)));

        assertThatThrownBy(() -> client.getProductDetail("6")).isInstanceOf(UpstreamUnavailableException.class);
    }

    @Test
    void throwsUpstreamUnavailableWhenDetailExceedsReadTimeout() {
        upstream.stubFor(get("/product/1000").willReturn(okJson("{}").withFixedDelay(1_000)));

        assertThatThrownBy(() -> client.getProductDetail("1000")).isInstanceOf(UpstreamUnavailableException.class);
    }
}
