package com.cism.backend.dto.stall;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OwnerStallDto(
    Long id,
    UserModel user,
    List<MealsModel> meals,
    List<SnacksModel> snacks,
    List<DrinksModel> drinks,
    IncomesModel incomes
) {
     public record UserModel(
        Long id,
        Long stallId,
        String name,
        String description,
        String image,
        Boolean status,
        String openAt,
        String closeAt,
        Instant createdAt,
        Instant updatedAt
    ) {}

    public record MealsModel(
        Long id,
        Long stallId,
        BigDecimal price,
        String name,
        String image,
        Integer stocks,
        Instant createdAt,
        Instant updatedAt
    ) {
    }

    public record SnacksModel(
        Long id,
        Long stallId,
        BigDecimal price,
        String name,
        String image,
        Integer stocks,
        Instant createdAt,
        Instant updatedAt
    ) {
    }

    public record DrinksModel(
        Long id,
        Long stallId,
        BigDecimal price,
        String name,
        String image,
        Integer stocks,
        Instant createdAt,
        Instant updatedAt
    ) {
    }

    public record IncomesModel(
        Long id,
        Long stallId,
        BigDecimal income,
        Instant earnedAt,
        Instant createdAt
    ) {}

}
