package com.cism.backend.dto.stall;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.web.multipart.MultipartFile;

public record RequestItemDto(
                String name,
                BigDecimal price,
                String image,
                MultipartFile imageFile,
                String category,
                Integer stocks,
                List<RequestItemVariationDto> variations) {

}
