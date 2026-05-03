package com.cism.backend.repository.stalls;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cism.backend.model.stalls.StallItemModel;

public interface StallItemRepository extends JpaRepository<StallItemModel, Long> {

    List<StallItemModel> findByStallId(Long stallId);
}
