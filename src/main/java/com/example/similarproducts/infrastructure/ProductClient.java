package com.example.similarproducts.infrastructure;

import com.example.similarproducts.domain.ProductDetail;
import com.example.similarproducts.domain.ProductNotFoundException;
import com.example.similarproducts.domain.UpstreamUnavailableException;
import java.util.List;
import java.util.function.Supplier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Client for the existing product API. Translates HTTP outcomes into domain exceptions:
 * 404 becomes {@link ProductNotFoundException}; 5xx, timeouts and I/O errors become {@link UpstreamUnavailableException}.
 */
@Component
public class ProductClient {

    private final RestClient restClient;

    public ProductClient(RestClient productApiRestClient) {
        this.restClient = productApiRestClient;
    }

    public List<String> getSimilarIds(String productId) {
        // The upstream sends numeric ids although the contract declares strings; Jackson coerces them to String
        String[] ids = call(productId, () -> restClient.get()
                .uri("/product/{productId}/similarids", productId)
                .retrieve()
                .onStatus(status -> status.isSameCodeAs(HttpStatus.NOT_FOUND), (request, response) -> {
                    throw new ProductNotFoundException(productId);
                })
                .body(String[].class));
        return ids == null ? List.of() : List.of(ids);
    }

    public ProductDetail getProductDetail(String productId) {
        return call(productId, () -> restClient.get()
                .uri("/product/{productId}", productId)
                .retrieve()
                .onStatus(status -> status.isSameCodeAs(HttpStatus.NOT_FOUND), (request, response) -> {
                    throw new ProductNotFoundException(productId);
                })
                .body(ProductDetail.class));
    }

    private static <T> T call(String productId, Supplier<T> request) {
        try {
            return request.get();
        } catch (RestClientException e) {
            throw new UpstreamUnavailableException("Product API failed for product " + productId, e);
        }
    }
}
