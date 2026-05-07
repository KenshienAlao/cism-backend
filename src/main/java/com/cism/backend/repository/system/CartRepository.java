package com.cism.backend.repository.system;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.cism.backend.model.system.review.CartModel;

@Repository
public interface CartRepository extends JpaRepository<CartModel, Long> {
    List<CartModel> findByUsersId(Long userId);
    Optional<CartModel> findByUsersIdAndStallItemIdAndVariationId(Long userId, Long stallItemId, Long variationId);
}
