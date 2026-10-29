# 8. Remember products that timed out for 30 seconds

[Español](0008-remember-slow-products.es.md)

- Status: accepted
- Date: 2026-10-29

## Context

A product whose detail takes longer than the 3 s read timeout is left out (ADR 3). A timeout is a failure, so it is
not cached (ADR 5): every later request that includes that product calls it again and waits the full 3 s, only to
leave it out once more. With the exercise's mocks, `/product/2` and `/product/3` always took about 3 s, even on a
warm cache. The circuit breaker does not solve this: one consistently slow product is not enough to open it, and if it
did, it would also cut off the healthy products.

## Options

1. Keep it: every request pays the timeout of every slow product.
2. Let the call finish in the background after the response and cache the result. It brings back products that are
   slow but under a longer limit (1000, 5 s), not the very slow ones (10000, 50 s), and it keeps more connections open
   under load.
3. Remember for a short while that a product timed out, and skip it without calling the product API.

## Decision

Option 3. When a product detail call ends in a **read timeout**, the product is stored in a `slowProducts` Caffeine
cache for **30 s** (`product-api.slow-product-ttl`). While it is there, `getProductDetail` throws
`UpstreamUnavailableException` straight away and the service leaves the product out, as with any failure.

- Only read timeouts count. A 5xx or a connect timeout says nothing about that product being slow, and the circuit
  breaker already covers an unhealthy product API.
- The skip is thrown, not returned as empty: an empty result would be cached as "not found" for the full 5 minutes.
- It is a Spring cache, so `SPRING_CACHE_TYPE=none` turns it off together with the other caches.

## Consequences

- From the second request on, `/product/2` and `/product/3` answer in a few milliseconds instead of about 3 s.
- A slow product that recovers is still left out for up to 30 s.
- With several instances, each one learns on its own which products are slow.
