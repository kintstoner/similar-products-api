package com.example.similarproducts.application;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * @param lookupTimeout upper bound for a single product detail lookup, as a safety net over the HTTP read timeout
 */
@ConfigurationProperties("similar-products")
public record SimilarProductsProperties(@DefaultValue("4s") Duration lookupTimeout) {
}
