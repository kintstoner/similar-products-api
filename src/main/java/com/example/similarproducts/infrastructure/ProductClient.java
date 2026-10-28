package com.example.similarproducts.infrastructure;

import com.example.similarproducts.domain.ProductDetail;
import com.example.similarproducts.domain.ProductNotFoundException;
import com.example.similarproducts.domain.UpstreamUnavailableException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Client for the existing product API. Translates HTTP outcomes into domain results: a missing product is
 * {@link ProductNotFoundException} for similar ids and an empty {@link Optional} for details; 5xx, timeouts,
 * I/O errors and an open circuit become {@link UpstreamUnavailableException}.
 * <p>
 * Successful answers are cached, including "product not found" (negative caching): a missing product will not
 * appear in the next few minutes. Failures are never cached. The cache sits in front of the circuit breakers, so
 * cached products are still served while a circuit is open.
 * <p>
 * Each endpoint has its own circuit breaker. There is deliberately no retry: under load, retrying against an
 * already slow service multiplies the traffic that is hurting it.
 */
@Component
public class ProductClient {

    private final RestClient restClient;
    private final CircuitBreaker similarIdsCircuitBreaker;
    private final CircuitBreaker productDetailCircuitBreaker;

    public ProductClient(RestClient productApiRestClient, CircuitBreakerRegistry circuitBreakers) {
        this.restClient = productApiRestClient;
        this.similarIdsCircuitBreaker = circuitBreakers.circuitBreaker("productSimilarIds");
        this.productDetailCircuitBreaker = circuitBreakers.circuitBreaker("productDetail");
    }

    @Cacheable(CacheNames.SIMILAR_IDS)
    public List<String> getSimilarIds(String productId) {
        // The upstream sends numeric ids although the contract declares strings; Jackson coerces them to String
        String[] ids = call(similarIdsCircuitBreaker, productId, () -> restClient.get()
                .uri("/product/{productId}/similarids", productId)
                .retrieve()
                .onStatus(status -> status.isSameCodeAs(HttpStatus.NOT_FOUND), (request, response) -> {
                    throw new ProductNotFoundException(productId);
                })
                .body(String[].class));
        return ids == null ? List.of() : List.of(ids);
    }

    /**
     * @return the product detail, or empty when the product does not exist (Spring caches the empty result too)
     */
    @Cacheable(CacheNames.PRODUCT_DETAILS)
    public Optional<ProductDetail> getProductDetail(String productId) {
        try {
            return Optional.ofNullable(call(productDetailCircuitBreaker, productId, () -> restClient.get()
                    .uri("/product/{productId}", productId)
                    .retrieve()
                    .onStatus(status -> status.isSameCodeAs(HttpStatus.NOT_FOUND), (request, response) -> {
                        throw new ProductNotFoundException(productId);
                    })
                    .body(ProductDetail.class)));
        } catch (ProductNotFoundException e) {
            return Optional.empty();
        }
    }

    private static <T> T call(CircuitBreaker circuitBreaker, String productId, Supplier<T> request) {
        try {
            return circuitBreaker.executeSupplier(() -> {
                try {
                    return request.get();
                } catch (RestClientException e) {
                    throw new UpstreamUnavailableException("Product API failed for product " + productId, e);
                }
            });
        } catch (CallNotPermittedException e) {
            throw new UpstreamUnavailableException("Circuit " + circuitBreaker.getName() + " is open", e);
        }
    }
}
