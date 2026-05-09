package com.cism.backend.dto.system.chat;

import java.time.LocalDateTime;

public record CustomerSearchResponse(
    Long customerId,
    String customerName,
    String customerImage,
    String lastMessage,
    LocalDateTime lastMessageAt,
    boolean hasThread
) {}
