package com.mosugar.recur.controller;

import com.mosugar.recur.dto.request.SyncRequest;
import com.mosugar.recur.dto.response.SyncResponse;
import com.mosugar.recur.exception.ResourceNotFoundException;
import com.mosugar.recur.repository.UserRepository;
import com.mosugar.recur.service.SyncService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/sync")
public class SyncController {

    private final SyncService syncService;
    private final UserRepository userRepository;

    public SyncController(SyncService syncService, UserRepository userRepository) {
        this.syncService = syncService;
        this.userRepository = userRepository;
    }

    @PostMapping
    public ResponseEntity<SyncResponse> sync(
            @AuthenticationPrincipal UserDetails principal,
            @Valid @RequestBody SyncRequest request) {
        UUID userId = userRepository.findByEmail(principal.getUsername())
                .map(u -> u.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return ResponseEntity.ok(syncService.sync(userId, request));
    }
}
