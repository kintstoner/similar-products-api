# 8. Recordar durante 30 segundos los productos que dieron timeout

[English](0008-remember-slow-products.md)

- Estado: aceptada
- Fecha: 2026-10-29

## Contexto

Un producto cuyo detalle tarda más que el read timeout de 3 s se omite (ADR 3). Un timeout es un fallo, así que no se
cachea (ADR 5): cada petición posterior que incluye ese producto lo vuelve a llamar y espera los 3 s enteros, solo
para omitirlo otra vez. Con los mocks del enunciado, `/product/2` y `/product/3` tardaban siempre unos 3 s, incluso
con la caché caliente. El circuit breaker no lo resuelve: un único producto lento no basta para abrirlo y, si lo
abriera, cortaría también los productos sanos.

## Opciones

1. Dejarlo así: cada petición paga el timeout de cada producto lento.
2. Dejar que la llamada termine en segundo plano después de responder y cachear el resultado. Recupera productos
   lentos pero por debajo de un límite mayor (1000, 5 s), no los muy lentos (10000, 50 s), y mantiene más conexiones
   abiertas cuando hay carga.
3. Recordar un rato que un producto dio timeout y omitirlo sin llamar al API de productos.

## Decisión

Opción 3. Cuando la llamada al detalle de un producto termina en **read timeout**, el producto se guarda en una caché
Caffeine `slowProducts` durante **30 s** (`product-api.slow-product-ttl`). Mientras está ahí, `getProductDetail` lanza
`UpstreamUnavailableException` al momento y el servicio omite el producto, como con cualquier fallo.

- Solo cuentan los read timeouts. Un 5xx o un timeout de conexión no dicen que ese producto sea lento, y el circuit
  breaker ya cubre un API de productos con problemas.
- La omisión se lanza como excepción, no se devuelve como vacío: un resultado vacío se cachearía como "no existe"
  durante los 5 minutos enteros.
- Es una caché de Spring, así que `SPRING_CACHE_TYPE=none` la desactiva junto con las demás.

## Consecuencias

- Desde la segunda petición, `/product/2` y `/product/3` responden en pocos milisegundos en vez de en unos 3 s.
- Un producto lento que se recupera sigue omitido hasta 30 s.
- Un producto lento ya no mantiene abierto el circuito de detalle. Con la ráfaga de k6 eso cuesta una espera de 3 s por
  cada producto lento nuevo: la media pasa de 18,6 ms a 47 ms y el p95 de 36 ms a 40 ms (README, prueba de carga).
- Con varias instancias, cada una aprende por su cuenta qué productos son lentos.
