package com.dvm.rune_server_backend.controller;

import com.dvm.rune_server_backend.common.Result;
import com.dvm.rune_server_backend.model.dto.LoginRequest;
import com.dvm.rune_server_backend.model.dto.RegisterRequest;
import com.dvm.rune_server_backend.model.entity.Users;
import com.dvm.rune_server_backend.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author 25826
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @PostMapping("/register")
    public Result<?> registerUser(@Valid @RequestBody RegisterRequest request) {
        return userService.register(request.getUsername(), request.getPassword());
    }
    @PostMapping("/login")
    public Result<?> loginUser(@Valid @RequestBody LoginRequest request) {
        return userService.login(request.getUsername(), request.getPassword());
    }
}
