package com.cism.backend.service.stalls;

import java.time.Instant;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.cism.backend.dto.stall.RequestMealDto;
import com.cism.backend.dto.stall.ResponseMealDto;
import com.cism.backend.exception.BadrequestException;
import com.cism.backend.exception.UnauthorizedException;
import com.cism.backend.model.admin.StallModel;
import com.cism.backend.model.stalls.StallMealsModel;
import com.cism.backend.repository.admin.CreateStallRepository;
import com.cism.backend.repository.stalls.StallMealsRepository;

import jakarta.transaction.Transactional;

@Service
public class MealStallService {
    
    @Autowired
    private CreateStallRepository createStallRepository;

    @Autowired
    private StallMealsRepository stallMealsRepository;



    @Transactional
    public ResponseMealDto createNewMealService(RequestMealDto entity) {
        String licence = getCurrentUserLicence();
        StallModel stall = createStallRepository.findByLicence(licence).orElseThrow(() -> new BadrequestException("Stall not found", "STALL_NOT_FOUND"));
        
        
        StallMealsModel meal = StallMealsModel.builder()
            .stall(stall)
            .name(entity.name())
            .price(entity.price())
            .image(entity.image())
            .stocks(entity.stocks())
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

         StallMealsModel savedMeal = stallMealsRepository.save(meal);

        return new ResponseMealDto(
            savedMeal.getId(),
            savedMeal.getStall().getId(),
            savedMeal.getName(),
            savedMeal.getPrice(),
            savedMeal.getImage(),
            savedMeal.getStocks(),
            savedMeal.getCreatedAt(),
            savedMeal.getUpdatedAt()
        );
    }

    @Transactional
    public ResponseMealDto updateMealService(Long id, RequestMealDto entity) {
        String licence = getCurrentUserLicence();
        StallModel stall = createStallRepository.findByLicence(licence).orElseThrow(() -> new BadrequestException("Stall not found", "STALL_NOT_FOUND"));
        StallMealsModel meal = stallMealsRepository.findById(id).orElseThrow(() -> new BadrequestException("Meal not found", "MEAL_NOT_FOUND"));
        
        if (meal.getStall().getId() != stall.getId()) {
            throw new BadrequestException("You do not have permission to update this meal", "UNAUTHORIZED");
        }

        meal.setName(entity.name());
        meal.setPrice(entity.price());
        meal.setImage(entity.image());
        meal.setStocks(entity.stocks());
        meal.setUpdatedAt(Instant.now());

        StallMealsModel updatedMeal = stallMealsRepository.save(meal);

        return new ResponseMealDto(
            updatedMeal.getId(),
            updatedMeal.getStall().getId(),
            updatedMeal.getName(),
            updatedMeal.getPrice(),
            updatedMeal.getImage(),
            updatedMeal.getStocks(),
            updatedMeal.getCreatedAt(),
            updatedMeal.getUpdatedAt()
        );
    }



    public String getCurrentUserLicence(){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if(auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            throw new UnauthorizedException("Stall owner not authenticated", "STALL_OWNER_NOT_AUTHENTICATED");
        }
        return auth.getName();
    }

    public boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
