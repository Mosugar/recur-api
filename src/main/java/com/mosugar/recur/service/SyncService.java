package com.mosugar.recur.service;

import com.mosugar.recur.dto.request.SyncRequest;
import com.mosugar.recur.dto.response.SyncResponse;

import java.util.UUID;

public interface SyncService {
    SyncResponse sync(UUID userId, SyncRequest request);
}
