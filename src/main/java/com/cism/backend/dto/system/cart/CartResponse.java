package com.cism.backend.dto.system.cart;

import java.math.BigDecimal;

public record CartResponse(
        Long id,
        Long itemId,
        Long variationId,
        String name,
        BigDecimal price,
        String image,
        String stallName,
        Integer quantity) {
}
