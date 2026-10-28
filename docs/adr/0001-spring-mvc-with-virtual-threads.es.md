# 1. Spring MVC sobre hilos virtuales en lugar de WebFlux

[English](0001-spring-mvc-with-virtual-threads.md)

- Estado: aceptada
- Fecha: 2026-10-28

## Contexto

Cada petición genera una llamada para obtener los ids similares y otra por cada producto similar. Las llamadas
pueden tardar desde milisegundos hasta 50 segundos, y la prueba de carga mantiene 200 usuarios virtuales (~400 req/s).
Con el modelo clásico de un hilo por petición, los hilos bloqueados en I/O lenta agotarían el pool.

## Opciones

1. **Spring MVC + hilos de plataforma**: código simple, pero los hilos bloqueados agotan el pool.
2. **Spring WebFlux (Reactor)**: no bloqueante y escala bien, pero todas las capas pasan a ser reactivas
   (`Mono`/`Flux`), más difíciles de leer, depurar y probar.
3. **Spring MVC + hilos virtuales (Java 21)**: código imperativo; una petición bloqueada en I/O aparca un hilo
   virtual barato en lugar de ocupar un hilo del sistema operativo.

## Decisión

Opción 3: `spring.threads.virtual.enabled=true`, `RestClient` sobre el `HttpClient` del JDK, y las consultas de
detalle en paralelo con `CompletableFuture` sobre un executor de un hilo virtual por tarea.

## Consecuencias

- Misma escalabilidad que WebFlux para esta carga limitada por I/O, con código imperativo normal.
- Coherente con el Java público de Inditex (Spring bloqueante; su librería `scs-outbox` declara que no soporta
  programación reactiva) y con Java 21, el que usan en su workshop de selección.
- Matiz en Java 21: los bloques `synchronized` fijan el hilo portador. Por eso la caché no usa
  `@Cacheable(sync = true)`, que bloquearía dentro de un compute sincronizado durante una llamada HTTP (ver ADR 5).
