package com.scjc.ndt.controller;

import com.scjc.ndt.common.R;
import com.scjc.ndt.dto.LoginRequest;
import com.scjc.ndt.dto.LoginResponse;
import com.scjc.ndt.dto.UserInfo;
import com.scjc.ndt.service.AuthService;
import com.scjc.ndt.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    @PostMapping("/login")
    public R<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return R.ok(authService.login(request));
    }

    @GetMapping("/me")
    public R<UserInfo> me(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return R.ok(userService.getById(userId));
    }
}
