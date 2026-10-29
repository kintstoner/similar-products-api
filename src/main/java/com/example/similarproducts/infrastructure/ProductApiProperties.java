package com.example.similarproducts.infrastructure;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Connection settings for the existing product API (similar ids and product detail).
 *
 * @param baseUrl        root URL of the product API
 * @param connectTimeout time allowed to open a TCP connection
 * @param readTimeout    time allowed for a response; a slow product is dropped rather than delaying the whole answer
 * @param slowProductTtl how long a product whose detail timed out is skipped without calling the product API again
 */
@ConfigurationProperties("product-api")
public record ProductApiProperties(
        @DefaultValue("http://localhost:3001") String baseUrl,
        @DefaultValue("1s") Duration connectTimeout,
        @DefaultValue("3s") Duration readTimeout,
        @DefaultValue("30s") Duration slowProductTtl) {
}
