# 4. Circuit breaker per downstream endpoint, without retry

[Español](0004-circuit-breaker-without-retry.es.md)

- Status: accepted
- Date: 2026-10-28

## Context

When the product API is slow or failing, each request keeps waiting up to the timeout and keeps adding load to a
service that is already struggling.

## Decision

- One Resilience4j circuit breaker per endpoint: `productSimilarIds` and `productDetail`. It opens when 50 % of the
  last 100 calls fail (minimum 20 calls) and probes again after 5 s. Applied programmatically through the
  `CircuitBreakerRegistry` inside `ProductClient`, so an open circuit is translated into the same domain exception as
  any other upstream failure.
- "Product not found" is ignored by the breaker: a missing product is a valid answer, not a sign of an unhealthy API.
- **No retry.** At ~400 req/s, retrying against a slow service multiplies the traffic that is hurting it, and a retry
  after a 3 s timeout would exceed the response-time budget anyway.

## Consequences

- In the "slow" and "verySlow" scenarios the detail breaker opens after the first timeouts: later requests skip the
  slow products immediately instead of waiting 3 s each. Cached products are still served (see ADR 5).
- Because cached products never reach the breaker, it only sees uncached calls; while it is open, an uncached healthy
  product is also left out for up to 5 s. A per-product bulkhead would avoid this at the cost of more complexity.
- Breaker state is exposed in `/actuator/health` and `/actuator/circuitbreakers`.
