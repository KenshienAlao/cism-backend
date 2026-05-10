package com.cism.backend.dto.system.review;

import org.springframework.web.multipart.MultipartFile;

public record ReviewRequest(
                Long stallId,
                Long itemId,
                String comment,
                Integer star,
                MultipartFile image) {

}
