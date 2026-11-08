package com.mosugar.recur.service;

import com.mosugar.recur.dto.request.LoginRequest;
import com.mosugar.recur.dto.request.RegisterRequest;
import com.mosugar.recur.dto.response.AuthResponse;
import com.mosugar.recur.dto.response.UserResponse;
import org.springframework.security.core.userdetails.UserDetails;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    UserResponse me(UserDetails principal);
}
