package com.example.similarproducts.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.similarproducts.domain.ProductCatalog;
import com.example.similarproducts.domain.ProductDetail;
import com.example.similarproducts.domain.ProductNotFoundException;
import com.example.similarproducts.domain.UpstreamUnavailableException;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class SimilarProductsServiceTest {

    private final ProductCatalog productCatalog = mock(ProductCatalog.class);
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
    private final MeterRegistry meterRegistry = new SimpleMeterRegistry();

    @AfterEach
    void shutDownExecutor() {
        // Interrupts the lookup left hanging by the timeout test instead of waiting for it
        executor.shutdownNow();
    }

    @Test
    void keepsSimilarityOrderWhenAnEarlierProductAnswersLast() {
        when(productCatalog.getSimilarIds("1")).thenReturn(List.of("2", "3", "4"));
        when(productCatalog.getProductDetail("2")).thenAnswer(invocation -> {
            Thread.sleep(100);
            return Optional.of(product("2"));
        });
        when(productCatalog.getProductDetail("3")).thenReturn(Optional.of(product("3")));
        when(productCatalog.getProductDetail("4")).thenReturn(Optional.of(product("4")));

        assertThat(service(Duration.ofSeconds(1)).getSimilarProducts("1"))
                .extracting(ProductDetail::id)
                .containsExactly("2", "3", "4");
    }

    @Test
    void looksUpEachDuplicatedIdOnceKeepingItsFirstPosition() {
        when(productCatalog.getSimilarIds("1")).thenReturn(List.of("3", "2", "3"));
        when(productCatalog.getProductDetail("2")).thenReturn(Optional.of(product("2")));
        when(productCatalog.getProductDetail("3")).thenReturn(Optional.of(product("3")));

        assertThat(service(Duration.ofSeconds(1)).getSimilarProducts("1"))
                .extracting(ProductDetail::id)
                .containsExactly("3", "2");
        verify(productCatalog).getProductDetail("3");
    }

    @Test
    void leavesOutProductsThatDoNotExist() {
        when(productCatalog.getSimilarIds("4")).thenReturn(List.of("1", "5"));
        when(productCatalog.getProductDetail("1")).thenReturn(Optional.of(product("1")));
        when(productCatalog.getProductDetail("5")).thenReturn(Optional.empty());

        assertThat(service(Duration.ofSeconds(1)).getSimilarProducts("4"))
                .extracting(ProductDetail::id)
                .containsExactly("1");
        assertThat(leftOut("missing")).isEqualTo(1);
        assertThat(leftOut("failed")).isZero();
    }

    @Test
    void leavesOutProductsWhoseLookupFails() {
        when(productCatalog.getSimilarIds("5")).thenReturn(List.of("1", "6", "7"));
        when(productCatalog.getProductDetail("1")).thenReturn(Optional.of(product("1")));
        when(productCatalog.getProductDetail("6"))
                .thenThrow(new UpstreamUnavailableException("500", null));
        when(productCatalog.getProductDetail("7")).thenThrow(new IllegalStateException("unexpected"));

        assertThat(service(Duration.ofSeconds(1)).getSimilarProducts("5"))
                .extracting(ProductDetail::id)
                .containsExactly("1");
        assertThat(leftOut("failed")).isEqualTo(2);
        assertThat(leftOut("missing")).isZero();
    }

    @Test
    void stopsWaitingForALookupThatExceedsTheLookupTimeout() {
        when(productCatalog.getSimilarIds("3")).thenReturn(List.of("100", "10000"));
        when(productCatalog.getProductDetail("100")).thenReturn(Optional.of(product("100")));
        when(productCatalog.getProductDetail("10000")).thenAnswer(invocation -> {
            Thread.sleep(5_000);
            return Optional.of(product("10000"));
        });

        long start = System.nanoTime();
        List<ProductDetail> result = service(Duration.ofMillis(200)).getSimilarProducts("3");
        long elapsedMillis = (System.nanoTime() - start) / 1_000_000;

        assertThat(result).extracting(ProductDetail::id).containsExactly("100");
        assertThat(elapsedMillis).isLessThan(1_000);
    }

    @Test
    void returnsAnEmptyListWhenThereAreNoSimilarIds() {
        when(productCatalog.getSimilarIds("1")).thenReturn(List.of());

        assertThat(service(Duration.ofSeconds(1)).getSimilarProducts("1")).isEmpty();
    }

    @Test
    void propagatesProductNotFoundForTheRequestedProduct() {
        when(productCatalog.getSimilarIds("99")).thenThrow(new ProductNotFoundException("99"));

        assertThatThrownBy(() -> service(Duration.ofSeconds(1)).getSimilarProducts("99"))
                .isInstanceOf(ProductNotFoundException.class);
        verify(productCatalog, never()).getProductDetail("99");
    }

    private SimilarProductsService service(Duration lookupTimeout) {
        return new SimilarProductsService(productCatalog, executor, new SimilarProductsProperties(lookupTimeout),
                meterRegistry);
    }

    private double leftOut(String reason) {
        Counter counter = meterRegistry.find("similar.products.left.out").tag("reason", reason).counter();
        return counter == null ? 0 : counter.count();
    }

    private static ProductDetail product(String id) {
        return new ProductDetail(id, "Product " + id, new BigDecimal("9.99"), true);
    }
}
