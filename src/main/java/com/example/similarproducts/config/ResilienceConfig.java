package com.example.similarproducts.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.core.registry.EntryAddedEvent;
import io.github.resilience4j.core.registry.EntryRemovedEvent;
import io.github.resilience4j.core.registry.EntryReplacedEvent;
import io.github.resilience4j.core.registry.RegistryEventConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ResilienceConfig {

    private static final Logger log = LoggerFactory.getLogger(ResilienceConfig.class);

    /**
     * Logs every circuit breaker state change: an opening circuit means products are being left out of responses.
     */
    @Bean
    RegistryEventConsumer<CircuitBreaker> circuitBreakerTransitionLogger() {
        return new RegistryEventConsumer<>() {

            @Override
            public void onEntryAddedEvent(EntryAddedEvent<CircuitBreaker> event) {
                CircuitBreaker circuitBreaker = event.getAddedEntry();
                circuitBreaker.getEventPublisher().onStateTransition(transition -> log.warn(
                        "Circuit breaker {} changed from {} to {}", circuitBreaker.getName(),
                        transition.getStateTransition().getFromState(), transition.getStateTransition().getToState()));
            }

            @Override
            public void onEntryRemovedEvent(EntryRemovedEvent<CircuitBreaker> event) {
                // Nothing to clean up: the breaker and its publisher go away together
            }

            @Override
            public void onEntryReplacedEvent(EntryReplacedEvent<CircuitBreaker> event) {
                // Breakers are never replaced at runtime in this service
            }
        };
    }
}
