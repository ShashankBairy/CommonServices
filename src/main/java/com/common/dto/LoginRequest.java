package com.common.dto;

import lombok.Data;

@Data
public class LoginRequest {
    private int empid;
    private String password;
}
