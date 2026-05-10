package com.cism.backend.dto.system.order;

import java.math.BigDecimal;

public record OrderItemResponse(
    Long id,
    String itemName,
    String variationName,
    Integer quantity,
    BigDecimal priceAtPurchase,
    String image,
    Long itemId
) {}
