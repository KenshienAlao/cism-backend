package com.cism.backend.dto.stall;

import java.math.BigDecimal;
import java.time.Instant;

public record ResponseItemDto(
        Long id,
        Long stallId,
        String name,
        BigDecimal price,
        String image,
        String category,
        Integer stocks,
        Integer sold,
        Integer previousSold,
        Instant createdAt,
        Instant updatedAt) {
}
