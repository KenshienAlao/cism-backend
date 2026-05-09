package com.cism.backend.dto.system.chat;

import java.time.LocalDateTime;

public record ChatThreadResponse(
    Long stallId,
    String stallName,
    String stallImage,
    Long customerId,
    String customerName,
    String customerImage,
    String lastMessage,
    LocalDateTime lastMessageAt,
    boolean isUnread
) {}
