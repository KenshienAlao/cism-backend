package com.cism.backend.dto.stall;

import java.math.BigDecimal;

public record RequestItemDto(
        String name,
        BigDecimal price,
        String image,
        String category,
        Integer stocks) {

}
