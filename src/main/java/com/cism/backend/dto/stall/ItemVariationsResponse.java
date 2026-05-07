package com.cism.backend.dto.stall;

import java.math.BigDecimal;

public record ItemVariationsResponse(
                Long id,
                String name,
                Integer stock,
                BigDecimal price,
                String image) {

}
