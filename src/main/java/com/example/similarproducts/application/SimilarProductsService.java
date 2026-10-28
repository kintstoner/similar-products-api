package com.example.similarproducts.application;

import com.example.similarproducts.domain.ProductDetail;
import com.example.similarproducts.infrastructure.ProductClient;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import org.springframework.stereotype.Service;

@Service
public class SimilarProductsService {

    private final ProductClient productClient;
    private final ExecutorService productLookupExecutor;

    public SimilarProductsService(ProductClient productClient, ExecutorService productLookupExecutor) {
        this.productClient = productClient;
        this.productLookupExecutor = productLookupExecutor;
    }

    /**
     * Returns the details of the products similar to the given one, ordered by similarity.
     * Details are fetched in parallel; results are collected in the original id order, not in completion order.
     */
    public List<ProductDetail> getSimilarProducts(String productId) {
        List<String> similarIds = productClient.getSimilarIds(productId).stream()
                .distinct()
                .toList();

        List<CompletableFuture<ProductDetail>> lookups = similarIds.stream()
                .map(id -> CompletableFuture.supplyAsync(() -> productClient.getProductDetail(id), productLookupExecutor))
                .toList();

        return lookups.stream()
                .map(CompletableFuture::join)
                .toList();
    }
}
