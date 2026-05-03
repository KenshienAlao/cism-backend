package com.cism.backend.dto.users;

import java.math.BigDecimal;
import java.util.List;

public record AllStallDto(
                Long id,
                String name,
                String description,
                String image,
                String openAt,
                String closeAt,
                String role,
                Boolean status,
                List<Review> reviews,
                List<Item> items) {

        public record Review(
                        Long id,
                        Long itemId,
                        Integer star,
                        String comment,
                        java.time.Instant createdAt) {
        }

        public record Item(
                        Long id,
                        Long stallId,
                        String name,
                        BigDecimal price,
                        Integer stocks,
                        String image,
                        String category) {
        }

}
