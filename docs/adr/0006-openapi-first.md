# 6. OpenAPI-first: the controller implements an interface generated from the contract

[Español](0006-openapi-first.es.md)

- Status: accepted
- Date: 2026-10-28

## Context

The front-end teams and this service agreed a contract (`similarProducts.yaml`). Hand-written controllers and DTOs
can drift from it silently.

## Decision

`openapi-generator-maven-plugin` generates the `ProductApi` interface and the `ProductDetail` DTO at build time from
the unchanged contract; `SimilarProductsController` implements that interface. Generated code lives in `target/` and
is not committed. Swagger UI (`/swagger-ui/index.html`) renders the contract file itself.

## Consequences

- A contract change that breaks the controller fails the build.
- The generated return type is `Set<ProductDetail>` because the contract declares `uniqueItems`; the controller uses a
  `LinkedHashSet` to keep similarity order.
- The domain uses its own `ProductDetail` record, so the API model and the upstream model can evolve separately.
