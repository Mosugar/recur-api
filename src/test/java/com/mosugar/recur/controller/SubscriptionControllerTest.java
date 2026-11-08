package com.mosugar.recur.controller;

import com.mosugar.recur.config.SecurityConfig;
import com.mosugar.recur.dto.request.SubscriptionRequest;
import com.mosugar.recur.dto.response.SubscriptionResponse;
import com.mosugar.recur.entity.User;
import com.mosugar.recur.enums.BillingCycle;
import com.mosugar.recur.enums.Category;
import com.mosugar.recur.enums.Currency;
import com.mosugar.recur.exception.ResourceNotFoundException;
import com.mosugar.recur.repository.UserRepository;
import com.mosugar.recur.security.JwtService;
import com.mosugar.recur.security.UserDetailsServiceImpl;
import com.mosugar.recur.service.SubscriptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@WebMvcTest(SubscriptionController.class)
@Import(SecurityConfig.class)
class SubscriptionControllerTest {

    @Autowired MockMvcTester mvc;
    @MockitoBean SubscriptionService subscriptionService;
    @MockitoBean UserRepository userRepository;
    @MockitoBean JwtService jwtService;
    @MockitoBean UserDetailsServiceImpl userDetailsService;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID SUB_ID  = UUID.randomUUID();
    private static final String USER_EMAIL = "alice@test.com";

    private SubscriptionResponse sampleResponse;

    @BeforeEach
    void setUp() {
        User mockUser = User.builder().id(USER_ID).email(USER_EMAIL).build();
        when(userRepository.findByEmail(USER_EMAIL)).thenReturn(Optional.of(mockUser));

        sampleResponse = new SubscriptionResponse(
                SUB_ID, "Netflix", 999L, Currency.EUR, BillingCycle.MONTHLY,
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 2, 1),
                Category.STREAMING, "#6366F1", null, false, true,
                OffsetDateTime.now(), OffsetDateTime.now());
    }

    // ── list ──────────────────────────────────────────────────────────────────

    @Test
    @WithMockUser(username = USER_EMAIL)
    void list_authenticated_returns200() {
        when(subscriptionService.list(any(), any(), any(), any()))
                .thenReturn(List.of(sampleResponse));

        assertThat(mvc.get().uri("/api/v1/subscriptions"))
                .hasStatus(200)
                .bodyJson().extractingPath("$[0].name").isEqualTo("Netflix");
    }

    @Test
    void list_unauthenticated_returns401() {
        assertThat(mvc.get().uri("/api/v1/subscriptions"))
                .hasStatus(401);
    }

    // ── getById ───────────────────────────────────────────────────────────────

    @Test
    @WithMockUser(username = USER_EMAIL)
    void getById_found_returns200() {
        when(subscriptionService.getById(USER_ID, SUB_ID)).thenReturn(sampleResponse);

        assertThat(mvc.get().uri("/api/v1/subscriptions/{id}", SUB_ID))
                .hasStatus(200)
                .bodyJson().extractingPath("$.id").isEqualTo(SUB_ID.toString());
    }

    @Test
    @WithMockUser(username = USER_EMAIL)
    void getById_otherUser_returns404() {
        when(subscriptionService.getById(any(), any()))
                .thenThrow(new ResourceNotFoundException("Subscription not found"));

        assertThat(mvc.get().uri("/api/v1/subscriptions/{id}", UUID.randomUUID()))
                .hasStatus(404);
    }

    // ── create ────────────────────────────────────────────────────────────────

    @Test
    @WithMockUser(username = USER_EMAIL)
    void create_validRequest_returns201() {
        when(subscriptionService.create(eq(USER_ID), any())).thenReturn(sampleResponse);

        assertThat(mvc.post().uri("/api/v1/subscriptions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name":"Netflix","priceCents":999,"currency":"EUR",
                         "cycle":"MONTHLY","firstPaymentDate":"2024-01-01",
                         "category":"STREAMING","color":"#6366F1",
                         "reminderEnabled":false,"active":true}
                        """))
                .hasStatus(201)
                .bodyJson().extractingPath("$.name").isEqualTo("Netflix");
    }

    @Test
    @WithMockUser(username = USER_EMAIL)
    void create_invalidColor_returns400() {
        assertThat(mvc.post().uri("/api/v1/subscriptions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name":"Netflix","priceCents":999,"currency":"EUR",
                         "cycle":"MONTHLY","firstPaymentDate":"2024-01-01",
                         "category":"STREAMING","color":"invalid",
                         "reminderEnabled":false,"active":true}
                        """))
                .hasStatus(400)
                .bodyJson().extractingPath("$.errors.color").isNotNull();
    }

    @Test
    @WithMockUser(username = USER_EMAIL)
    void create_missingName_returns400() {
        assertThat(mvc.post().uri("/api/v1/subscriptions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"priceCents":999,"currency":"EUR","cycle":"MONTHLY",
                         "firstPaymentDate":"2024-01-01","category":"STREAMING",
                         "color":"#6366F1","reminderEnabled":false,"active":true}
                        """))
                .hasStatus(400);
    }

    // ── delete ────────────────────────────────────────────────────────────────

    @Test
    @WithMockUser(username = USER_EMAIL)
    void delete_found_returns204() {
        assertThat(mvc.delete().uri("/api/v1/subscriptions/{id}", SUB_ID))
                .hasStatus(204);
    }

    @Test
    @WithMockUser(username = USER_EMAIL)
    void delete_notFound_returns404() {
        org.mockito.Mockito.doThrow(new ResourceNotFoundException("not found"))
                .when(subscriptionService).delete(any(), any());

        assertThat(mvc.delete().uri("/api/v1/subscriptions/{id}", UUID.randomUUID()))
                .hasStatus(404);
    }
}
