package com.cism.backend.repository.stalls;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cism.backend.model.stalls.StallMealsModel;

public interface StallMealsRepository extends JpaRepository<StallMealsModel, Long> {}
