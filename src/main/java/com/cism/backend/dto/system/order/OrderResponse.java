package com.cism.backend.dto.system.order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderResponse(
    String id,
    String orderCode,
    BigDecimal subtotal,
    BigDecimal deliveryFee,
    BigDecimal totalAmount,
    String deliveryMethod,
    String paymentMethod,
    String status,
    String note,
    String cancelReason,
    String cancelledBy,
    Instant createdAt,
    String stallName,
    String stallImage,
    Long stallId,
    List<OrderItemResponse> orderItems
) {}
