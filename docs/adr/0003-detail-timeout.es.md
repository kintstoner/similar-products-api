# 3. Timeout de tres segundos por detalle de producto, aplicado por el cliente HTTP

[English](0003-detail-timeout.md)

- Estado: aceptada
- Fecha: 2026-10-28

## Contexto

Algunos detalles tardan 1 s, 5 s o 50 s. La respuesta solo puede ser tan rápida como el detalle más lento que se espere.

## Opciones

1. Sin timeout (o uno largo, como 6 s, "para que salgan todos"): la respuesta tarda lo que el producto más lento;
   con 50 s, el cliente de la prueba de carga abandona a los 30 s.
2. Timeout corto: algunos productos lentos se omiten, pero la respuesta siempre es rápida.

## Decisión

Opción 2 con **3 s por defecto** (`product-api.read-timeout`), como decisión de producto: un widget de "similares"
no debe bloquear la ficha; mejor menos resultados rápido que todos tarde. El timeout es el read timeout del cliente
HTTP, de modo que la llamada cancelada libera de verdad su conexión. `CompletableFuture.orTimeout`
(`similar-products.lookup-timeout`, 4 s) es solo una red de seguridad.

## Consecuencias

- Los productos 1000 (5 s) y 10000 (50 s) quedan fuera de los escenarios "slow" y "verySlow". Es intencionado.
- El peor tiempo de respuesta queda acotado a unos 3 s más la llamada de ids similares.
