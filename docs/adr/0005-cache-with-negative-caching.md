# 5. Caffeine cache with negative caching, in front of the circuit breaker

[Español](0005-cache-with-negative-caching.es.md)

- Status: accepted
- Date: 2026-10-28

## Context

The same products are requested again and again. Every repeated call costs latency and load on the product API.

## Decision

- In-memory Caffeine caches for similar ids (`similarIds`) and product details (`productDetails`):
  5 minutes TTL, 10,000 entries maximum, configurable through `spring.cache.caffeine.spec`.
- **Negative caching**: `getProductDetail` returns `Optional.empty()` for a 404 and Spring caches that empty result,
  so a missing product is not asked for again for 5 minutes.
- Failures (5xx, timeouts, open circuit) are never cached: they are thrown, not returned.
- The cache sits in front of the circuit breaker (the `@Cacheable` proxy wraps `ProductClient`).
- No `@Cacheable(sync = true)`: on Java 21 it would pin virtual threads while one of them performs a blocking HTTP call.
  The cost is a short burst of duplicate calls for the same product on a cold cache.

## Consequences

- After the first request, most lookups in the load test are served from memory.
- Prices and availability can be up to 5 minutes stale. With several instances each has its own cache; a shared
  cache (for example Redis) would be the next step if consistency across instances mattered.
