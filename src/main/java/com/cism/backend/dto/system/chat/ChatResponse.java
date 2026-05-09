package com.cism.backend.dto.system.chat;

import java.time.LocalDateTime;

public record ChatResponse(
    Long id,
    Long senderId,
    String senderName,
    Long stallId,
    Long customerId,
    String customerName,
    String content,
    boolean isRead,
    boolean isDeleted,
    LocalDateTime createdAt
) {}
