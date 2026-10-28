package com.example.similarproducts.infrastructure;

import com.example.similarproducts.domain.ProductDetail;
import java.math.BigDecimal;
import java.util.Optional;

/**
 * Product detail exactly as the product API sends it. Every field is nullable here, so an incomplete answer can be
 * detected instead of being forwarded and breaking the contract, which declares all four fields as required.
 */
record ProductDetailResponse(String id, String name, BigDecimal price, Boolean availability) {

    Optional<ProductDetail> toDomain() {
        if (isBlank(id) || isBlank(name) || price == null || availability == null) {
            return Optional.empty();
        }
        return Optional.of(new ProductDetail(id, name, price, availability));
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
