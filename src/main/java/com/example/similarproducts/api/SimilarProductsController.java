package com.example.similarproducts.api;

import com.example.similarproducts.api.generated.ProductApi;
import com.example.similarproducts.application.SimilarProductsService;
import com.example.similarproducts.domain.ProductDetail;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SimilarProductsController implements ProductApi {

    private final SimilarProductsService similarProductsService;

    public SimilarProductsController(SimilarProductsService similarProductsService) {
        this.similarProductsService = similarProductsService;
    }

    @Override
    public ResponseEntity<Set<com.example.similarproducts.api.generated.model.ProductDetail>> getProductSimilar(
            String productId) {
        // LinkedHashSet keeps the similarity order while honouring the contract's uniqueItems
        Set<com.example.similarproducts.api.generated.model.ProductDetail> body =
                similarProductsService.getSimilarProducts(productId).stream()
                        .map(SimilarProductsController::toResponse)
                        .collect(Collectors.toCollection(LinkedHashSet::new));
        return ResponseEntity.ok(body);
    }

    private static com.example.similarproducts.api.generated.model.ProductDetail toResponse(ProductDetail product) {
        return new com.example.similarproducts.api.generated.model.ProductDetail(
                product.id(), product.name(), product.price(), product.availability());
    }
}
