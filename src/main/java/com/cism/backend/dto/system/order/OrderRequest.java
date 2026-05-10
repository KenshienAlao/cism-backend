package com.cism.backend.dto.system.order;

import java.util.List;

public record OrderRequest(
        List<Long> cartItemIds,
        BuyNowItem buyNowItem,
        String deliveryMethod,
        String paymentMethod,
        String note) {
    public record BuyNowItem(
            Long stallId,
            Long itemId,
            Long variationId,
            Integer quantity) {
    }
}
