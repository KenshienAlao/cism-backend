package com.cism.backend.dto.system.chat;

import java.time.LocalDateTime;

public record ChatResponse(
    Long id,
    String conversationId,
    Long senderId,
    String senderName,
    Long stallId,
    Long customerId,
    String customerName,
    String content,
    boolean readByCustomer,
    boolean readByStall,
    boolean isDeleted,
    boolean sentByStall,
    LocalDateTime createdAt
) {}
