package com.common.dto;

import lombok.AllArgsConstructor;
import lombok.Data;


@Data
@AllArgsConstructor
public class JwtResponse {
    private final String accessToken;
    private final long expiresAtEpochSeconds;
    private final String tokenType;
    
}

