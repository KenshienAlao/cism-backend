package com.cism.backend.dto.system.cart;

public record CartRequest(
        Long stallId,
        Long stallItemId,
        Long variationId,
        Integer quantity) {
}
