package com.mosugar.recur.controller;

import com.mosugar.recur.dto.request.SubscriptionRequest;
import com.mosugar.recur.dto.response.SubscriptionResponse;
import com.mosugar.recur.enums.Category;
import com.mosugar.recur.repository.UserRepository;
import com.mosugar.recur.exception.ResourceNotFoundException;
import com.mosugar.recur.service.SubscriptionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/subscriptions")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;
    private final UserRepository userRepository;

    public SubscriptionController(SubscriptionService subscriptionService,
                                  UserRepository userRepository) {
        this.subscriptionService = subscriptionService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<List<SubscriptionResponse>> list(
            @AuthenticationPrincipal UserDetails principal,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) Category category,
            @RequestParam(required = false) String sort) {
        UUID userId = resolveUserId(principal);
        return ResponseEntity.ok(subscriptionService.list(userId, active, category, sort));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SubscriptionResponse> getById(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable UUID id) {
        UUID userId = resolveUserId(principal);
        return ResponseEntity.ok(subscriptionService.getById(userId, id));
    }

    @PostMapping
    public ResponseEntity<SubscriptionResponse> create(
            @AuthenticationPrincipal UserDetails principal,
            @Valid @RequestBody SubscriptionRequest request) {
        UUID userId = resolveUserId(principal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(subscriptionService.create(userId, request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SubscriptionResponse> update(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable UUID id,
            @Valid @RequestBody SubscriptionRequest request) {
        UUID userId = resolveUserId(principal);
        return ResponseEntity.ok(subscriptionService.update(userId, id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable UUID id) {
        UUID userId = resolveUserId(principal);
        subscriptionService.delete(userId, id);
        return ResponseEntity.noContent().build();
    }

    private UUID resolveUserId(UserDetails principal) {
        return userRepository.findByEmail(principal.getUsername())
                .map(u -> u.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
