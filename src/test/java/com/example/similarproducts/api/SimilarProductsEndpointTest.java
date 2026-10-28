package com.example.similarproducts.api;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class SimilarProductsEndpointTest {

    @RegisterExtension
    static WireMockExtension upstream = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @DynamicPropertySource
    static void upstreamProperties(DynamicPropertyRegistry registry) {
        registry.add("product-api.base-url", upstream::baseUrl);
        registry.add("product-api.read-timeout", () -> "1s");
    }

    @Autowired
    private MockMvc mockMvc;

    @Test
    void returnsSimilarProductsInSimilarityOrderEvenWhenAnEarlierOneIsSlower() throws Exception {
        stubSimilarIds("1", "[2,3,4]");
        stubProduct("2", 0);
        stubProduct("3", 200);
        stubProduct("4", 0);

        mockMvc.perform(get("/product/1/similar"))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        [
                          {"id":"2","name":"Product 2","price":2.5,"availability":true},
                          {"id":"3","name":"Product 3","price":2.5,"availability":true},
                          {"id":"4","name":"Product 4","price":2.5,"availability":true}
                        ]""", JsonCompareMode.STRICT));
    }

    @Test
    void fetchesProductDetailsInParallel() throws Exception {
        stubSimilarIds("1", "[2,3,4]");
        stubProduct("2", 300);
        stubProduct("3", 300);
        stubProduct("4", 300);

        long start = System.nanoTime();
        mockMvc.perform(get("/product/1/similar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));
        long elapsedMillis = (System.nanoTime() - start) / 1_000_000;

        assertThat(elapsedMillis).isLessThan(800);
    }

    @Test
    void deduplicatesSimilarIdsKeepingFirstOccurrence() throws Exception {
        stubSimilarIds("1", "[3,2,3]");
        stubProduct("2", 0);
        stubProduct("3", 0);

        mockMvc.perform(get("/product/1/similar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id").value(contains("3", "2")));
    }

    @Test
    void returnsEmptyListWhenThereAreNoSimilarProducts() throws Exception {
        stubSimilarIds("1", "[]");

        mockMvc.perform(get("/product/1/similar"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void returns404WhenTheProductDoesNotExist() throws Exception {
        upstream.stubFor(WireMock.get("/product/99/similarids").willReturn(aResponse().withStatus(404)));

        mockMvc.perform(get("/product/99/similar"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType("application/problem+json"))
                .andExpect(jsonPath("$.detail").value("Product not found: 99"));
    }

    @Test
    void dropsSimilarProductsThatAreNotFound() throws Exception {
        stubSimilarIds("4", "[1,2,5]");
        stubProduct("1", 0);
        stubProduct("2", 0);
        upstream.stubFor(WireMock.get("/product/5").willReturn(aResponse().withStatus(404)));

        mockMvc.perform(get("/product/4/similar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id").value(contains("1", "2")));
    }

    @Test
    void dropsSimilarProductsWhoseDetailFails() throws Exception {
        stubSimilarIds("5", "[1,2,6]");
        stubProduct("1", 0);
        stubProduct("2", 0);
        upstream.stubFor(WireMock.get("/product/6").willReturn(aResponse().withStatus(500)));

        mockMvc.perform(get("/product/5/similar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id").value(contains("1", "2")));
    }

    @Test
    void dropsSimilarProductsSlowerThanTheReadTimeoutWithoutWaitingForThem() throws Exception {
        stubSimilarIds("2", "[3,100,1000]");
        stubProduct("3", 0);
        stubProduct("100", 200);
        stubProduct("1000", 5_000);

        long start = System.nanoTime();
        mockMvc.perform(get("/product/2/similar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id").value(contains("3", "100")));
        long elapsedMillis = (System.nanoTime() - start) / 1_000_000;

        assertThat(elapsedMillis).isLessThan(2_000);
    }

    @Test
    void returns503WhenSimilarIdsAreUnavailable() throws Exception {
        upstream.stubFor(WireMock.get("/product/1/similarids").willReturn(aResponse().withStatus(500)));

        mockMvc.perform(get("/product/1/similar"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().contentType("application/problem+json"));
    }

    private static void stubSimilarIds(String productId, String body) {
        upstream.stubFor(WireMock.get("/product/" + productId + "/similarids").willReturn(okJson(body)));
    }

    private static void stubProduct(String id, int delayMillis) {
        upstream.stubFor(WireMock.get("/product/" + id).willReturn(okJson("""
                {"id":"%s","name":"Product %s","price":2.5,"availability":true}""".formatted(id, id))
                .withFixedDelay(delayMillis)));
    }
}
