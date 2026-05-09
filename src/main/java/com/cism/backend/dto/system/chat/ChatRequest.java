package com.cism.backend.dto.system.chat;

public record ChatRequest(
    String conversationId,
    Long stallId,
    Long customerId,
    String content
) {}
