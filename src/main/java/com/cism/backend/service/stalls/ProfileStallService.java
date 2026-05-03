package com.cism.backend.service.stalls;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.cism.backend.dto.stall.OwnerStallDto;
import com.cism.backend.exception.BadrequestException;
import com.cism.backend.model.admin.StallModel;
import com.cism.backend.model.stalls.StallIncomesModel;
import com.cism.backend.model.stalls.StallItemModel;
import com.cism.backend.model.stalls.StallUsersModel;
import com.cism.backend.model.system.review.ReviewModel;
import com.cism.backend.repository.admin.CreateStallIncomesRepository;
import com.cism.backend.repository.admin.CreateStallRepository;
import com.cism.backend.util.CurrentUserLicence;

import jakarta.transaction.Transactional;

@Service
public class ProfileStallService {
    @Autowired
    CreateStallRepository createStallRepository;

    @Autowired
    CreateStallIncomesRepository createStallIncomesRepository;

    @Autowired
    CurrentUserLicence currentUserLicence;

    @Transactional
    public OwnerStallDto getUserService() {
        String licence = currentUserLicence.getCurrentUserLicence();

        StallModel stall = createStallRepository.findByLicence(licence)
                .orElseThrow(() -> new BadrequestException("Stall not found", "STALL_NOT_FOUND"));

        List<StallItemModel> items = stall.getItemList();

        boolean isBusiness = stall.getUserList().stream().findFirst()
                .map(u -> "BUSINESS".equalsIgnoreCase(u.getRole()))
                .orElse(false);

        List<OwnerStallDto.MealsModel> meals = items.stream()
                .filter(i -> (isBusiness ? "ID_LANCE" : "meal").equalsIgnoreCase(i.getCategory()))
                .map(this::mapMeal)
                .toList();

        List<OwnerStallDto.SnacksModel> snacks = items.stream()
                .filter(i -> (isBusiness ? "TSHIRT" : "snack").equalsIgnoreCase(i.getCategory()))
                .map(this::mapSnacks)
                .toList();

        List<OwnerStallDto.DrinksModel> drinks = items.stream()
                .filter(i -> (isBusiness ? "PANTS" : "drink").equalsIgnoreCase(i.getCategory()))
                .map(this::mapDrinks)
                .toList();

        return new OwnerStallDto(
                stall.getId(),
                stall.getUserList().stream().findFirst().map(this::mapUser)
                        .orElseThrow(() -> new BadrequestException("User not found", "USER_NOT_FOUND")),
                meals,
                snacks,
                drinks,
                stall.getReviewList().stream().map(this::mapReviews).toList(),
                stall.getIncomeList().stream().findFirst().map(this::mapIncomes).orElse(null),
                calculateRevenueTrend(stall));
    }

    private OwnerStallDto.TrendDto calculateRevenueTrend(StallModel stall) {
        java.math.BigDecimal currentPeriod = createStallIncomesRepository
                .sumIncomeByStallAndDateRange(stall, java.time.Instant.now().minus(java.time.Duration.ofDays(7)), java.time.Instant.now())
                .orElse(java.math.BigDecimal.ZERO);

        java.math.BigDecimal previousPeriod = createStallIncomesRepository
                .sumIncomeByStallAndDateRange(stall, java.time.Instant.now().minus(java.time.Duration.ofDays(14)), java.time.Instant.now().minus(java.time.Duration.ofDays(7)))
                .orElse(java.math.BigDecimal.ZERO);

        double percentageChange = 0;
        String trend = "neutral";

        if (previousPeriod.compareTo(java.math.BigDecimal.ZERO) > 0) {
            java.math.BigDecimal diff = currentPeriod.subtract(previousPeriod);
            percentageChange = diff.divide(previousPeriod, 4, java.math.RoundingMode.HALF_UP).doubleValue() * 100;
        } else if (currentPeriod.compareTo(java.math.BigDecimal.ZERO) > 0) {
            percentageChange = 100.0;
        }

        if (percentageChange > 0)
            trend = "up";
        else if (percentageChange < 0)
            trend = "down";

        return new OwnerStallDto.TrendDto(
                currentPeriod,
                previousPeriod,
                Math.abs(percentageChange),
                trend);
    }

    private OwnerStallDto.UserModel mapUser(StallUsersModel u) {
        return new OwnerStallDto.UserModel(
                u.getId(),
                u.getStall().getId(),
                u.getName(),
                u.getDescription(),
                u.getImage(),
                u.getStatus(),
                u.getOpenAt(),
                u.getCloseAt(),
                u.getRole(),
                u.getCreatedAt(),
                u.getUpdatedAt());
    }

    private OwnerStallDto.MealsModel mapMeal(StallItemModel m) {
        return new OwnerStallDto.MealsModel(
                m.getId(),
                m.getStall().getId(),
                m.getPrice(),
                m.getName(),
                m.getImage(),
                m.getStocks(),
                m.getSold(),
                m.getPreviousSold(),
                m.getCreatedAt(),
                m.getUpdatedAt());
    }

    private OwnerStallDto.SnacksModel mapSnacks(StallItemModel s) {
        return new OwnerStallDto.SnacksModel(
                s.getId(),
                s.getStall().getId(),
                s.getPrice(),
                s.getName(),
                s.getImage(),
                s.getStocks(),
                s.getSold(),
                s.getPreviousSold(),
                s.getCreatedAt(),
                s.getUpdatedAt());
    }

    private OwnerStallDto.DrinksModel mapDrinks(StallItemModel d) {
        return new OwnerStallDto.DrinksModel(
                d.getId(),
                d.getStall().getId(),
                d.getPrice(),
                d.getName(),
                d.getImage(),
                d.getStocks(),
                d.getSold(),
                d.getPreviousSold(),
                d.getCreatedAt(),
                d.getUpdatedAt());
    }

    private OwnerStallDto.ReviewModel mapReviews(ReviewModel r) {
        return new OwnerStallDto.ReviewModel(
                r.getId(),
                r.getItemId(),
                r.getUsers().getId(),
                r.getStar(),
                r.getComment(),
                r.getCreateAt());
    }

    private OwnerStallDto.IncomesModel mapIncomes(StallIncomesModel i) {
        return new OwnerStallDto.IncomesModel(
                i.getId(),
                i.getStall().getId(),
                i.getIncome(),
                i.getEarnedAt(),
                i.getCreatedAt());
    }
}
