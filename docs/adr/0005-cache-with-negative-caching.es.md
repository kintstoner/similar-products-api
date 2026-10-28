# 5. Caché Caffeine con caché negativa, delante del circuit breaker

[English](0005-cache-with-negative-caching.md)

- Estado: aceptada
- Fecha: 2026-10-28

## Contexto

Se piden los mismos productos una y otra vez. Cada llamada repetida cuesta latencia y carga sobre el API de productos.

## Decisión

- Cachés Caffeine en memoria para ids similares (`similarIds`) y detalles (`productDetails`): TTL de 5 minutos,
  máximo 10.000 entradas, configurable con `spring.cache.caffeine.spec`.
- **Caché negativa**: `getProductDetail` devuelve `Optional.empty()` ante un 404 y Spring cachea ese resultado vacío,
  así un producto inexistente no se vuelve a pedir en 5 minutos.
- Los fallos (5xx, timeouts, circuito abierto) nunca se cachean: se lanzan como excepción, no se devuelven.
- La caché va delante del circuit breaker (el proxy de `@Cacheable` envuelve a `ProductClient`).
- Sin `@Cacheable(sync = true)`: en Java 21 fijaría hilos virtuales mientras uno de ellos hace una llamada HTTP
  bloqueante. El coste es una pequeña ráfaga de llamadas duplicadas para el mismo producto con la caché fría.

## Consecuencias

- Tras la primera petición, la mayoría de consultas de la prueba de carga se sirven desde memoria.
- Precio y disponibilidad pueden tener hasta 5 minutos de antigüedad. Con varias instancias cada una tiene su caché;
  una caché compartida (por ejemplo Redis) sería el siguiente paso si importara la coherencia entre instancias.
