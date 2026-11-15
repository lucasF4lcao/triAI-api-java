package com.tcc.triAI_api_java.controller;

import com.tcc.triAI_api_java.dto.request.LoginRequest;
import com.tcc.triAI_api_java.dto.response.IsLoggedInResponse;
import com.tcc.triAI_api_java.dto.response.UserResponse;
import com.tcc.triAI_api_java.entity.PersonalAccessToken;
import com.tcc.triAI_api_java.entity.User;
import com.tcc.triAI_api_java.service.auth.LoginService;
import com.tcc.triAI_api_java.service.auth.LogoutService;
import com.tcc.triAI_api_java.web.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class AuthController {

    private final LoginService loginService;
    private final LogoutService logoutService;

    public AuthController(LoginService loginService, LogoutService logoutService) {
        this.loginService = loginService;
        this.logoutService = logoutService;
    }

    @PostMapping("/login")
    public ApiResponse<?> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(loginService.run(request.email(), request.password()), "Usuário logado com sucesso");
    }

    @PostMapping("/logout")
    public ApiResponse<?> logout(HttpServletRequest request) {
        PersonalAccessToken token = (PersonalAccessToken) request.getAttribute("currentPersonalAccessToken");
        logoutService.run(token);
        return ApiResponse.success(null, "Usuário deslogado com sucesso");
    }

    @GetMapping("/user")
    public ApiResponse<?> user(@AuthenticationPrincipal User user) {
        return ApiResponse.success(UserResponse.from(user), "Usuário retornado com sucesso");
    }

    @GetMapping("/is-logged-in")
    public ApiResponse<?> isLoggedIn() {
        return ApiResponse.success(new IsLoggedInResponse(true), "Usuário logado");
    }
}
