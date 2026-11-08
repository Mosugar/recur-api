package com.mosugar.recur.controller;

import com.mosugar.recur.dto.request.LoginRequest;
import com.mosugar.recur.dto.request.RegisterRequest;
import com.mosugar.recur.dto.response.AuthResponse;
import com.mosugar.recur.dto.response.UserResponse;
import com.mosugar.recur.enums.Currency;
import com.mosugar.recur.exception.EmailAlreadyExistsException;
import com.mosugar.recur.security.JwtService;
import com.mosugar.recur.security.UserDetailsServiceImpl;
import com.mosugar.recur.service.AuthService;
import com.mosugar.recur.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerTest {

    @Autowired MockMvcTester mvc;
    // AuthController dependency
    @MockitoBean AuthService authService;
    // JwtAuthFilter (a @Component loaded by @WebMvcTest) needs its own deps
    @MockitoBean JwtService jwtService;
    @MockitoBean UserDetailsServiceImpl userDetailsService;

    // ── register ──────────────────────────────────────────────────────────────

    @Test
    void register_validRequest_returns201WithToken() {
        when(authService.register(any())).thenReturn(new AuthResponse("jwt-token"));

        assertThat(mvc.post().uri("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"alice@test.com","password":"password123","displayName":"Alice"}
                        """))
                .hasStatus(201)
                .bodyJson().extractingPath("$.token").isEqualTo("jwt-token");
    }

    @Test
    void register_shortPassword_returns400WithErrorField() {
        assertThat(mvc.post().uri("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"alice@test.com","password":"short","displayName":"Alice"}
                        """))
                .hasStatus(400)
                .bodyJson().extractingPath("$.errors.password").isNotNull();
    }

    @Test
    void register_invalidEmail_returns400WithErrorField() {
        assertThat(mvc.post().uri("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"not-an-email","password":"password123","displayName":"Alice"}
                        """))
                .hasStatus(400)
                .bodyJson().extractingPath("$.errors.email").isNotNull();
    }

    @Test
    void register_duplicateEmail_returns409() {
        when(authService.register(any()))
                .thenThrow(new EmailAlreadyExistsException("alice@test.com"));

        assertThat(mvc.post().uri("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"alice@test.com","password":"password123","displayName":"Alice"}
                        """))
                .hasStatus(409);
    }

    // ── login ─────────────────────────────────────────────────────────────────

    @Test
    void login_validCredentials_returns200WithToken() {
        when(authService.login(any())).thenReturn(new AuthResponse("jwt-token"));

        assertThat(mvc.post().uri("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"alice@test.com","password":"password123"}
                        """))
                .hasStatus(200)
                .bodyJson().extractingPath("$.token").isEqualTo("jwt-token");
    }

    @Test
    void login_missingPassword_returns400() {
        assertThat(mvc.post().uri("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"alice@test.com"}
                        """))
                .hasStatus(400);
    }

    // ── me ────────────────────────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "alice@test.com")
    void me_authenticated_returns200() {
        when(authService.me(any())).thenReturn(new UserResponse(
                UUID.randomUUID(), "alice@test.com", "Alice",
                Currency.EUR, OffsetDateTime.now()));

        assertThat(mvc.get().uri("/api/v1/auth/me"))
                .hasStatus(200)
                .bodyJson().extractingPath("$.email").isEqualTo("alice@test.com");
    }

    @Test
    void me_unauthenticated_returns401() {
        assertThat(mvc.get().uri("/api/v1/auth/me"))
                .hasStatus(401);
    }
}
