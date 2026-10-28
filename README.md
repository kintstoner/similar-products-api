# similar-products-api

[Español](README.es.md)

Spring Boot service that exposes `GET /product/{productId}/similar` on port **5000**: it returns the details of the
products similar to a given one, **ordered by similarity**, aggregating two existing downstream APIs
(`/product/{id}/similarids` and `/product/{id}`).

> Work in progress. This README grows with each milestone.

## Stack

- Java 21, Spring Boot 4.1, Maven (wrapper included, no global install needed)
- Spring MVC on **virtual threads**
- **OpenAPI-first**: the controller interface and DTOs are generated from the agreed contract
  (`src/main/resources/openapi/similarProducts.yaml`, copied unchanged from the exercise)

## Run

```bash
./mvnw spring-boot:run
```

## Test

```bash
./mvnw verify
```
