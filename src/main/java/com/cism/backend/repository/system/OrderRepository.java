package com.cism.backend.repository.system;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cism.backend.model.system.order.OrderModel;

public interface OrderRepository extends JpaRepository<OrderModel, String> {
    List<OrderModel> findByUser_IdAndDeletedByCustomerFalseOrderByCreatedAtDesc(Long userId);
    List<OrderModel> findByStall_IdAndDeletedByStallFalseOrderByCreatedAtDesc(Long stallId);
    List<OrderModel> findByStall_IdOrderByCreatedAtDesc(Long stallId);
    boolean existsByOrderCode(String orderCode);
}

