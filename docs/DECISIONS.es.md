# Decisiones de diseño

[English](DECISIONS.md)

Una línea por decisión; el razonamiento completo está en cada Architecture Decision Record.

| # | Decisión | Por qué, en una línea |
|---|---|---|
| [1](adr/0001-spring-mvc-with-virtual-threads.es.md) | Spring MVC sobre hilos virtuales, no WebFlux | Misma escalabilidad para I/O bloqueante con código imperativo normal |
| [2](adr/0002-partial-results.es.md) | Se omiten los similares que fallan; 404 solo si el producto no existe; 503 si fallan los ids | Mejor resultados parciales que ninguno |
| [3](adr/0003-detail-timeout.es.md) | Read timeout de 3 s por detalle de producto | Un widget de similares no debe bloquear la ficha |
| [4](adr/0004-circuit-breaker-without-retry.es.md) | Circuit breaker por endpoint, sin reintentos | Dejar de esperar a un servicio que falla; los reintentos multiplican la carga |
| [5](adr/0005-cache-with-negative-caching.es.md) | Caché Caffeine, también de 404, delante del breaker | Los productos repetidos salen de memoria, incluso con el circuito abierto |
| [6](adr/0006-openapi-first.es.md) | El controlador implementa una interfaz generada desde el contrato | La compilación falla si el código se separa del contrato acordado |
