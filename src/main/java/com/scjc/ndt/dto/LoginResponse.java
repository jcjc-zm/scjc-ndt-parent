package com.scjc.ndt.dto;

import lombok.Data;

@Data
public class LoginResponse {
    private String token;
    private UserInfo userInfo;
}
