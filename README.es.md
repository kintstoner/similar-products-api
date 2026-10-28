# similar-products-api

[English](README.md)

Servicio Spring Boot que expone `GET /product/{productId}/similar` en el puerto **5000**: devuelve el detalle de los
productos similares a uno dado, **ordenados por similitud**, agregando dos APIs existentes
(`/product/{id}/similarids` y `/product/{id}`).

> En construcción. Este README crece con cada hito.

## Stack

- Java 21, Spring Boot 4.1, Maven (incluye wrapper, no hace falta instalarlo)
- Spring MVC sobre **hilos virtuales**
- **OpenAPI-first**: la interfaz del controlador y los DTOs se generan desde el contrato acordado
  (`src/main/resources/openapi/similarProducts.yaml`, copiado sin cambios del enunciado)

## Ejecutar

```bash
./mvnw spring-boot:run
```

## Tests

```bash
./mvnw verify
```
