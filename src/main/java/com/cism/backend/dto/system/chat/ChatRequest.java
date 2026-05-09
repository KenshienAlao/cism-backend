package com.cism.backend.dto.system.chat;

public record ChatRequest(
    Long stallId,
    Long customerId, // Necessary if the sender is a stall owner replying to a specific customer
    String content
) {}
