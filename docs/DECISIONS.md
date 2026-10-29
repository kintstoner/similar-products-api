# Design decisions

[Español](DECISIONS.es.md)

One line per decision; the full reasoning is in each Architecture Decision Record.

| # | Decision | Why, in one line |
|---|---|---|
| [1](adr/0001-spring-mvc-with-virtual-threads.md) | Spring MVC on virtual threads, not WebFlux | Same scalability for blocking I/O with plain imperative code |
| [2](adr/0002-partial-results.md) | Leave out similar products that fail or arrive incomplete; 404 only for an unknown product; 503 if the ids fail | Partial results beat no results, and never break the contract |
| [3](adr/0003-detail-timeout.md) | 3 s read timeout per product detail | A similar-products widget must not hold the product page |
| [4](adr/0004-circuit-breaker-without-retry.md) | Circuit breaker per endpoint, no retry | Stop waiting on a failing service; retries multiply load |
| [5](adr/0005-cache-with-negative-caching.md) | Caffeine cache, 404 cached too, in front of the breaker | Repeated products are served from memory, even with the circuit open |
| [6](adr/0006-openapi-first.md) | Controller implements an interface generated from the contract | The build fails if the code drifts from the agreed contract |
| [7](adr/0007-bulkhead.md) | Bulkhead: at most 500 calls in flight per endpoint | Virtual threads remove the thread pool's natural brake; the dependency needs one |
| [8](adr/0008-remember-slow-products.md) | A product that timed out is skipped for 30 s | A consistently slow product must not cost every request the full timeout |
