package com.example.similarproducts.api;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class OperationalEndpointsTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void exposesHealthIncludingCircuitBreakers() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components.circuitBreakers.details.productDetail.status").exists());
    }

    @Test
    void exposesCacheMetrics() throws Exception {
        mockMvc.perform(get("/actuator/metrics/cache.gets"))
                .andExpect(status().isOk());
    }

    @Test
    void servesTheAgreedContractUnchanged() throws Exception {
        mockMvc.perform(get("/openapi/similarProducts.yaml"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("operationId: get-product-similar")));
    }

    @Test
    void servesSwaggerUi() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk());
    }
}
