package com.example.similarproducts.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

/**
 * Caffeine caches configured through {@code spring.cache.*} in application.yml.
 */
@Configuration
@EnableCaching
public class CacheConfig {
}
