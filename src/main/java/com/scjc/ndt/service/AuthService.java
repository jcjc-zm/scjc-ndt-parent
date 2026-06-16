package com.scjc.ndt.service;

import com.scjc.ndt.dto.LoginRequest;
import com.scjc.ndt.dto.LoginResponse;

public interface AuthService {
    LoginResponse login(LoginRequest request);
}
