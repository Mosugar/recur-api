package com.mosugar.recur.service.impl;

import com.mosugar.recur.dto.request.LoginRequest;
import com.mosugar.recur.dto.request.RegisterRequest;
import com.mosugar.recur.dto.response.AuthResponse;
import com.mosugar.recur.dto.response.UserResponse;
import com.mosugar.recur.entity.User;
import com.mosugar.recur.exception.EmailAlreadyExistsException;
import com.mosugar.recur.exception.ResourceNotFoundException;
import com.mosugar.recur.mapper.UserMapper;
import com.mosugar.recur.repository.UserRepository;
import com.mosugar.recur.security.JwtService;
import com.mosugar.recur.service.AuthService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final UserMapper userMapper;

    public AuthServiceImpl(UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           JwtService jwtService,
                           AuthenticationManager authenticationManager,
                           UserMapper userMapper) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.userMapper = userMapper;
    }

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.email().toLowerCase().strip();
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException(email);
        }
        User user = userRepository.save(User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(request.password()))
                .displayName(request.displayName().strip())
                .build());
        return new AuthResponse(jwtService.generateToken(user.getId(), user.getEmail()));
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        String email = request.email().toLowerCase().strip();
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, request.password()));
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return new AuthResponse(jwtService.generateToken(user.getId(), user.getEmail()));
    }

    @Override
    public UserResponse me(UserDetails principal) {
        User user = userRepository.findByEmail(principal.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return userMapper.toResponse(user);
    }
}
