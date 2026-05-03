package com.cism.backend.dto.admin;

import java.math.BigDecimal;
import java.time.Instant;

public record StallListResponse(
        Long id,
        String licence,
        UserModel user,
        IncomesModel incomes) {
    public record UserModel(
            Long id,
            Long stallId,
            String name,
            String description,
            String image,
            Boolean status,
            String openAt,
            String closeAt,
            String role,
            Instant createdAt,
            Instant updatedAt) {
    }

    public record IncomesModel(
            Long id,
            Long stallId,
            BigDecimal income,
            Instant earnedAt,
            Instant createdAt) {
    }

}
