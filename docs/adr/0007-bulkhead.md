# 7. Bulkhead: cap the calls in flight to the product API

[Español](0007-bulkhead.es.md)

- Status: accepted
- Date: 2026-10-28

## Context

Virtual threads make waiting almost free for this service (ADR 1). That removes the natural brake a thread pool
used to provide: if the product API slows down, nothing limits how many calls we keep open against it. The service
stays healthy while the dependency gets buried.

## Decision

- One Resilience4j semaphore bulkhead per endpoint (`productSimilarIds`, `productDetail`): at most **500 calls in
  flight**, waiting up to **200 ms** for a free slot. Beyond that the call is rejected and the product is left out
  (ADR 2). A semaphore, not a thread pool: it works with virtual threads without pinning them.
- Order: circuit breaker outside, bulkhead inside. An open circuit fails fast without taking a slot.
- The circuit breaker ignores bulkhead rejections: hitting our own cap says nothing about the product API's health.

## Consequences

- Worst case, the product API receives 500 concurrent calls per endpoint from each instance, instead of an unbounded
  number.
- The limit is sized for the load test: a few hundred concurrent detail calls at its peak, with the cache disabled.
  In production it should come from the dependency's real capacity.
