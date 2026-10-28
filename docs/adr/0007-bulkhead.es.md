# 7. Bulkhead: limitar las llamadas en vuelo al API de productos

[English](0007-bulkhead.md)

- Estado: aceptada
- Fecha: 2026-10-28

## Contexto

Con hilos virtuales, esperar no le cuesta casi nada a este servicio (ADR 1). Eso elimina el freno natural que antes
ponía el pool de hilos: si el API de productos se ralentiza, nada limita cuántas llamadas le mantenemos abiertas.
El servicio sigue sano mientras la dependencia se hunde.

## Decisión

- Un bulkhead de semáforo de Resilience4j por endpoint (`productSimilarIds`, `productDetail`): como máximo **500
  llamadas en vuelo**, esperando hasta **200 ms** por un hueco libre. Por encima, la llamada se rechaza y el producto
  se omite (ADR 2). Semáforo y no pool de hilos: funciona con hilos virtuales sin fijarlos al hilo portador.
- Orden: circuit breaker por fuera, bulkhead por dentro. Un circuito abierto falla al momento sin ocupar hueco.
- El circuit breaker ignora los rechazos del bulkhead: llegar a nuestro propio límite no dice nada de la salud del
  API de productos.

## Consecuencias

- En el peor caso, el API de productos recibe 500 llamadas concurrentes por endpoint desde cada instancia, en lugar
  de un número ilimitado.
- El límite está dimensionado para la prueba de carga: unos cientos de llamadas de detalle concurrentes en el pico,
  con la caché desactivada. En producción debería salir de la capacidad real de la dependencia.
