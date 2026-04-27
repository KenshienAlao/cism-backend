package com.cism.backend.dto.stall;

import com.cism.backend.model.admin.StallModel;

public record LoginStallResponseDto(
    String accessToken,
    String refreshToken,
    StallModel stall
) {
}
