package com.cism.backend.dto.stall;

import java.math.BigDecimal;
import java.time.Instant;

public record ResponseMealDto(
    Long id,
    Long stallId,
    String name,
    BigDecimal price,
    String image,
    Integer stocks,
    Instant createdAt,
    Instant updatedAt
) {
}
