package com.cism.backend.dto.users;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import com.cism.backend.dto.stall.ItemVariationsResponse;

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
                List<Item> items,
                Instant createdAt) {

        public record Review(
                        Long id,
                        Long itemId,
                        Long userId,
                        User user,
                        Integer star,
                        String comment,
                        java.time.Instant createdAt) {
        }

        public record User(
                        String clientName,
                        String avatar,
                        String role) {
        }

        public record Item(
                        Long id,
                        Long stallId,
                        String name,
                        BigDecimal price,
                        Integer stocks,
                        String image,
                        String category,
                        List<ItemVariationsResponse> variations) {
        }

}
