package com.example.similarproducts.config;

import com.example.similarproducts.infrastructure.ProductApiProperties;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.boot.cache.autoconfigure.CacheManagerCustomizer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Caffeine caches configured through {@code spring.cache.*} in application.yml, plus the short-lived memory of slow
 * products, which needs its own TTL.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    CacheManagerCustomizer<CaffeineCacheManager> slowProductsCache(ProductApiProperties properties) {
        return cacheManager -> cacheManager.registerCustomCache("slowProducts", Caffeine.newBuilder()
                .expireAfterWrite(properties.slowProductTtl())
                .maximumSize(10_000)
                .recordStats()
                .build());
    }
}
