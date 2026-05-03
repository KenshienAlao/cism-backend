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
    List<ReviewModel> reviews,
    IncomesModel incomes,
    TrendDto revenueTrend
) {
    public record TrendDto(
        BigDecimal currentPeriodTotal,
        BigDecimal previousPeriodTotal,
        double percentageChange,
        String trend // "up", "down", "neutral"
    ) {}

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
        Instant updatedAt
    ) {}

    public record MealsModel(
        Long id,
        Long stallId,
        BigDecimal price,
        String name,
        String image,
        Integer stocks,
        Integer sold,
        Integer previousSold,
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
        Integer sold,
        Integer previousSold,
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
        Integer sold,
        Integer previousSold,
        Instant createdAt,
        Instant updatedAt
    ) {
    }

    public record ReviewModel(
        Long id,
        Long itemId,
        Long userId,
        Integer star,
        String comment,
        Instant createdAt
    ) {}

    public record IncomesModel(
        Long id,
        Long stallId,
        BigDecimal income,
        Instant earnedAt,
        Instant createdAt
    ) {}

}
