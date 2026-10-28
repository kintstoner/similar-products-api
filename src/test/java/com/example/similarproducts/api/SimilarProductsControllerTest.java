package com.example.similarproducts.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.similarproducts.application.SimilarProductsService;
import com.example.similarproducts.domain.ProductDetail;
import com.example.similarproducts.domain.ProductNotFoundException;
import com.example.similarproducts.domain.UpstreamUnavailableException;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SimilarProductsController.class)
@ExtendWith(OutputCaptureExtension.class)
class SimilarProductsControllerTest {

    @Test
    void logsTheCauseOfA503(CapturedOutput output) throws Exception {
        when(similarProductsService.getSimilarProducts("1")).thenThrow(new UpstreamUnavailableException(
                "Product API failed for product 1", new IllegalStateException("connection reset")));

        mockMvc.perform(get("/product/1/similar")).andExpect(status().isServiceUnavailable());

        assertThat(output).contains("Product API failed for product 1").contains("connection reset");
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SimilarProductsService similarProductsService;

    @Test
    void returnsTheSimilarProductsInTheContractFormatAndOrder() throws Exception {
        when(similarProductsService.getSimilarProducts("1")).thenReturn(List.of(
                new ProductDetail("3", "Blazer", new BigDecimal("29.99"), false),
                new ProductDetail("2", "Dress", new BigDecimal("19.99"), true)));

        mockMvc.perform(get("/product/1/similar"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(content().json("""
                        [
                          {"id":"3","name":"Blazer","price":29.99,"availability":false},
                          {"id":"2","name":"Dress","price":19.99,"availability":true}
                        ]""", JsonCompareMode.STRICT));
    }

    @Test
    void mapsProductNotFoundTo404ProblemDetail() throws Exception {
        when(similarProductsService.getSimilarProducts("99")).thenThrow(new ProductNotFoundException("99"));

        mockMvc.perform(get("/product/99/similar"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType("application/problem+json"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("Product not found: 99"));
    }

    @Test
    void mapsUpstreamUnavailableTo503ProblemDetailWithoutLeakingInternals() throws Exception {
        when(similarProductsService.getSimilarProducts("1"))
                .thenThrow(new UpstreamUnavailableException("Product API failed for product 1", null));

        mockMvc.perform(get("/product/1/similar"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().contentType("application/problem+json"))
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.detail").value("Similar products are temporarily unavailable"));
    }
}
