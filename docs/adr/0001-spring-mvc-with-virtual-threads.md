# 1. Spring MVC on virtual threads instead of WebFlux

[Español](0001-spring-mvc-with-virtual-threads.es.md)

- Status: accepted
- Date: 2026-10-28

## Context

Each request fans out to one call for the similar ids plus one call per similar product. Downstream calls can take
from milliseconds to 50 seconds, and the load test keeps 200 virtual users busy (~400 req/s). On a classic
thread-per-request model, threads blocked on slow I/O would exhaust the pool.

## Options

1. **Spring MVC + platform threads**: simple code, but threads blocked on slow calls exhaust the pool.
2. **Spring WebFlux (Reactor)**: non-blocking, scales well, but every layer becomes reactive (`Mono`/`Flux`),
   which is harder to read, debug and test.
3. **Spring MVC + virtual threads (Java 21)**: imperative code; a request blocked on I/O parks a cheap virtual thread
   instead of holding an OS thread.

## Decision

Option 3: `spring.threads.virtual.enabled=true`, `RestClient` over the JDK `HttpClient`, and detail lookups run in
parallel with `CompletableFuture` on a virtual-thread-per-task executor.

## Consequences

- Same scalability as WebFlux for this I/O-bound workload, with plain imperative code.
- Consistent with Inditex's public Java code (blocking Spring; their `scs-outbox` library states it does not support
  reactive programming) and with Java 21, used in their recruitment workshop.
- Caveat on Java 21: `synchronized` blocks pin the carrier thread. This is why the cache does not use
  `@Cacheable(sync = true)`, which would block inside a synchronized compute during an HTTP call (see ADR 5).
