# 6. OpenAPI-first: el controlador implementa una interfaz generada desde el contrato

[English](0006-openapi-first.md)

- Estado: aceptada
- Fecha: 2026-10-28

## Contexto

Los equipos de front y este servicio acordaron un contrato (`similarProducts.yaml`). Controladores y DTOs escritos a
mano pueden desviarse de él sin que nadie lo note.

## Decisión

`openapi-generator-maven-plugin` genera en cada build la interfaz `ProductApi` y el DTO `ProductDetail` a partir del
contrato sin modificar; `SimilarProductsController` implementa esa interfaz. El código generado vive en `target/` y no
se versiona. Swagger UI (`/swagger-ui/index.html`) muestra el propio fichero del contrato.

## Consecuencias

- Un cambio de contrato que rompa el controlador hace fallar la compilación.
- El tipo de retorno generado es `Set<ProductDetail>` porque el contrato declara `uniqueItems`; el controlador usa un
  `LinkedHashSet` para mantener el orden de similitud.
- El dominio usa su propio record `ProductDetail`, así el modelo del API y el del servicio externo pueden evolucionar
  por separado.
