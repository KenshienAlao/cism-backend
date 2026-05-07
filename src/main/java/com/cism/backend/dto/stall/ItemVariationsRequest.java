package com.cism.backend.dto.stall;

import java.math.BigDecimal;
import org.springframework.web.multipart.MultipartFile;

public record ItemVariationsRequest(
        Long itemId,
        String name,
        BigDecimal price,
        Integer stocks,
        String image,
        MultipartFile imageFile) {

}
