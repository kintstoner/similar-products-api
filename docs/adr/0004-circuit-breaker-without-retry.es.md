# 4. Circuit breaker por endpoint, sin reintentos

[English](0004-circuit-breaker-without-retry.md)

- Estado: aceptada
- Fecha: 2026-10-28

## Contexto

Cuando el API de productos va lento o falla, cada petición espera hasta el timeout y añade carga a un servicio que
ya tiene problemas.

## Decisión

- Un circuit breaker de Resilience4j por endpoint: `productSimilarIds` y `productDetail`. Se abre cuando falla el
  50 % de las últimas 100 llamadas (mínimo 20) y vuelve a probar a los 5 s. Se aplica de forma programática con el
  `CircuitBreakerRegistry` dentro de `ProductClient`, así un circuito abierto se traduce a la misma excepción de
  dominio que cualquier otro fallo del API.
- El breaker ignora "producto no encontrado": un producto que no existe es una respuesta válida, no un síntoma de que
  el API esté mal.
- **Sin reintentos.** A ~400 req/s, reintentar contra un servicio lento multiplica el tráfico que lo está hundiendo,
  y un reintento tras un timeout de 3 s superaría de todas formas el presupuesto de tiempo de respuesta.

## Consecuencias

- En los escenarios "slow" y "verySlow" el breaker de detalle se abre tras los primeros timeouts: las peticiones
  siguientes descartan al momento los productos lentos en lugar de esperar 3 s. Los productos en caché se siguen
  sirviendo (ADR 5).
- Como los productos cacheados nunca llegan al breaker, este solo ve llamadas no cacheadas; mientras está abierto,
  un producto sano no cacheado también se omite durante hasta 5 s. Un bulkhead por producto lo evitaría a cambio de
  más complejidad.
- El estado del breaker se publica en `/actuator/health` y `/actuator/circuitbreakers`, y cada cambio de estado se
  registra como aviso en el log: un circuito que se abre significa que se están omitiendo productos.
- Cada endpoint tiene además un bulkhead que limita las llamadas en vuelo (ADR 7).
