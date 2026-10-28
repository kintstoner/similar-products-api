# 2. Resultados parciales: se omiten los similares que no se pueden obtener

[English](0002-partial-results.md)

- Estado: aceptada
- Fecha: 2026-10-28

## Contexto

El contrato solo define `200` (lista de similares) y `404` (producto no encontrado). Los mocks devuelven 404, 500 o
respuestas muy lentas para algunos similares. Fallar la lista entera porque falla un similar haría frágil la
funcionalidad.

## Decisión

- Un similar cuyo detalle devuelve 404, 5xx, supera el timeout o está bloqueado por un circuito abierto o un bulkhead
  lleno **se omite**; la respuesta es `200` con el resto, manteniendo el orden de similitud.
- También se omite un detalle al que le falte un campo obligatorio del contrato (`id`, `name`, `price`,
  `availability`), con un aviso en el log: reenviarlo rompería el contrato con nuestros propios clientes.
- Solo se devuelve `404` cuando el producto **pedido** no existe (la llamada de ids similares devuelve 404).
- Si no se pueden obtener los ids similares (5xx, timeout, circuito abierto) se responde `503` con un Problem Detail
  (RFC 9457). Está fuera del contrato acordado, pero sin ids no hay nada útil que devolver y el 503 indica al cliente
  que el fallo es temporal.
- Los ids duplicados se eliminan conservando su primera posición (el contrato declara `uniqueItems`).

## Consecuencias

- Los escenarios "notFound" y "error" de la prueba de carga responden `200` con dos productos en lugar de fallar.
- El cliente no sabe por la respuesta que se omitió un producto. Queda en la métrica `similar.products.left.out`,
  con la etiqueta `missing` (no existe o está incompleto) o `failed` (error, timeout, circuito abierto, bulkhead
  lleno), y en los logs.
- Un `503` se registra en el log con su causa, ya que el cliente solo recibe un mensaje genérico.
