package com.cism.backend.controller.stall;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cism.backend.dto.common.Api;
import com.cism.backend.dto.stall.RequestMealDto;
import com.cism.backend.dto.stall.ResponseMealDto;
import com.cism.backend.service.stalls.MealStallService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;



@RestController
@RequestMapping("/api/owner/stall/meals")
public class MealsStallController {

    private final MealStallService mealStallService;

    public MealsStallController(MealStallService mealStallService) {
        this.mealStallService = mealStallService;
    }
    
    @PostMapping("/create-new-meal")
    public ResponseEntity<Api<ResponseMealDto>> createNewMeal(@RequestBody RequestMealDto entity) throws Exception {

        ResponseMealDto success = mealStallService.createNewMealService(entity);

        return ResponseEntity.ok(Api.ok("Create new meal success", "CREATE_MEAL_SUCCESS", success));
    }

    @PutMapping("/update-meal/{id}")
    public ResponseEntity<Api<ResponseMealDto>> updateMeal(@PathVariable Long id, @RequestBody RequestMealDto entity) {
        ResponseMealDto success = mealStallService.updateMealService(id, entity);
        return ResponseEntity.ok(Api.ok("Update meal success", "UPDATE_MEAL_SUCCESS", null));
    }


}
