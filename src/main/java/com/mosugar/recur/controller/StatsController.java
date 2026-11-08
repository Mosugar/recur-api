package com.mosugar.recur.controller;

import com.mosugar.recur.dto.response.CategoryStatsResponse;
import com.mosugar.recur.dto.response.StatsSummaryResponse;
import com.mosugar.recur.dto.response.UpcomingPaymentResponse;
import com.mosugar.recur.exception.ResourceNotFoundException;
import com.mosugar.recur.repository.UserRepository;
import com.mosugar.recur.service.StatsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class StatsController {

    private final StatsService statsService;
    private final UserRepository userRepository;

    public StatsController(StatsService statsService, UserRepository userRepository) {
        this.statsService = statsService;
        this.userRepository = userRepository;
    }

    @GetMapping("/subscriptions/upcoming")
    public ResponseEntity<List<UpcomingPaymentResponse>> upcoming(
            @AuthenticationPrincipal UserDetails principal,
            @RequestParam(defaultValue = "7") int days) {
        UUID userId = resolveUserId(principal);
        return ResponseEntity.ok(statsService.upcoming(userId, days));
    }

    @GetMapping("/stats/summary")
    public ResponseEntity<StatsSummaryResponse> summary(
            @AuthenticationPrincipal UserDetails principal) {
        UUID userId = resolveUserId(principal);
        return ResponseEntity.ok(statsService.summary(userId));
    }

    @GetMapping("/stats/by-category")
    public ResponseEntity<List<CategoryStatsResponse>> byCategory(
            @AuthenticationPrincipal UserDetails principal) {
        UUID userId = resolveUserId(principal);
        return ResponseEntity.ok(statsService.byCategory(userId));
    }

    private UUID resolveUserId(UserDetails principal) {
        return userRepository.findByEmail(principal.getUsername())
                .map(u -> u.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
