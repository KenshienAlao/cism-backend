package com.cism.backend.repository.stalls;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cism.backend.model.stalls.StallItemModel;

public interface StallDrinksRepository extends JpaRepository<StallItemModel, Long> {
    
    Optional<StallItemModel>  findByStallId(Long stallId);
}
