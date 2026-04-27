package com.cism.backend.dto.stall;

import java.math.BigDecimal;

public record RequestMealDto(
    String name,
    BigDecimal price,
    String image,
    Integer stocks
) {
    
}
