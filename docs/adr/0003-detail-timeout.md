# 3. Three-second timeout per product detail, enforced by the HTTP client

[Español](0003-detail-timeout.es.md)

- Status: accepted
- Date: 2026-10-28

## Context

Some product details take 1 s, 5 s or 50 s. The response can only be as fast as the slowest detail we wait for.

## Options

1. No timeout (or a long one, such as 6 s, "so every product shows up"): the answer takes as long as the slowest
   product; with 50 s, the load-test client gives up at 30 s.
2. Short timeout: some slow products are left out, but the answer is always fast.

## Decision

Option 2 with **3 s by default** (`product-api.read-timeout`), as a product decision: a "similar products" widget
must not hold the product page; fewer results fast beat all results late. The timeout is the HTTP client's read
timeout, so a timed-out call really releases its connection. `CompletableFuture.orTimeout`
(`similar-products.lookup-timeout`, 4 s) is only a safety net on top.

## Consequences

- Product 1000 (5 s) and 10000 (50 s) are left out of the "slow" and "verySlow" scenarios. This is intended.
- The worst-case response time is bounded by about 3 s plus the similar-ids call.
