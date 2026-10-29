package com.example.similarproducts.domain;

import java.util.List;
import java.util.Optional;

/**
 * Port to the source of product data. The application layer depends on this interface; the adapter that talks to
 * the existing product API lives in infrastructure.
 */
public interface ProductCatalog {

    /**
     * Returns the ids of the products similar to the given one, ordered by similarity.
     *
     * @throws ProductNotFoundException if the product does not exist
     * @throws UpstreamUnavailableException if the product data cannot be obtained
     */
    List<String> getSimilarIds(String productId);

    /**
     * Returns the detail of a product, or empty if the product does not exist.
     *
     * @throws UpstreamUnavailableException if the product data cannot be obtained
     */
    Optional<ProductDetail> getProductDetail(String productId);
}
