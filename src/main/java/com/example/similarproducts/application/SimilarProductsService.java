package com.example.similarproducts.application;

import com.example.similarproducts.domain.ProductCatalog;
import com.example.similarproducts.domain.ProductDetail;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class SimilarProductsService {

    private static final Logger log = LoggerFactory.getLogger(SimilarProductsService.class);

    private final ProductCatalog productCatalog;
    private final ExecutorService productLookupExecutor;
    private final SimilarProductsProperties properties;
    private final Counter missingProducts;
    private final Counter failedProducts;

    public SimilarProductsService(ProductCatalog productCatalog, ExecutorService productLookupExecutor,
            SimilarProductsProperties properties, MeterRegistry meterRegistry) {
        this.productCatalog = productCatalog;
        this.productLookupExecutor = productLookupExecutor;
        this.properties = properties;
        // The response cannot say that a product was left out, so the metric is where it shows up
        this.missingProducts = leftOutCounter(meterRegistry, "missing");
        this.failedProducts = leftOutCounter(meterRegistry, "failed");
    }

    private static Counter leftOutCounter(MeterRegistry meterRegistry, String reason) {
        return Counter.builder("similar.products.left.out")
                .description("Similar products left out of a response")
                .tag("reason", reason)
                .register(meterRegistry);
    }

    /**
     * Returns the details of the products similar to the given one, ordered by similarity.
     * Details are fetched in parallel; results are collected in the original id order, not in completion order.
     * A similar product whose detail is missing, fails or is too slow is left out: partial results beat no results.
     */
    public List<ProductDetail> getSimilarProducts(String productId) {
        List<String> similarIds = productCatalog.getSimilarIds(productId).stream()
                .distinct()
                .toList();

        List<CompletableFuture<Optional<ProductDetail>>> lookups = similarIds.stream()
                .map(this::lookUpDetail)
                .toList();

        return lookups.stream()
                .map(CompletableFuture::join)
                .flatMap(Optional::stream)
                .toList();
    }

    private CompletableFuture<Optional<ProductDetail>> lookUpDetail(String id) {
        return CompletableFuture.supplyAsync(() -> productCatalog.getProductDetail(id), productLookupExecutor)
                .orTimeout(properties.lookupTimeout().toMillis(), TimeUnit.MILLISECONDS)
                .handle((detail, error) -> {
                    if (error != null) {
                        failedProducts.increment();
                        log.debug("Similar product {} left out: {}", id, error.toString());
                        return Optional.empty();
                    }
                    if (detail.isEmpty()) {
                        missingProducts.increment();
                    }
                    return detail;
                });
    }
}
