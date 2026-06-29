package com.vms.controller;

import com.vms.dto.BlacklistRequest;
import com.vms.dto.BlacklistResponse;
import com.vms.entity.Blacklist;
import com.vms.service.BlacklistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/blacklist")
@RequiredArgsConstructor
@Tag(name = "Blacklist Management", description = "Endpoints for managing blacklisted visitors")
public class BlacklistController {

    private final BlacklistService blacklistService;

    @PostMapping
    @Operation(summary = "Add a visitor to the blacklist")
    public ResponseEntity<BlacklistResponse> addToBlacklist(@Valid @RequestBody BlacklistRequest request) {
        Blacklist blacklist = blacklistService.addToBlacklist(request);
        return ResponseEntity.ok(mapToResponse(blacklist));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove a visitor from the blacklist")
    public ResponseEntity<Void> removeFromBlacklist(@PathVariable Long id) {
        blacklistService.removeFromBlacklist(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping
    @Operation(summary = "Get all blacklisted visitors")
    public ResponseEntity<Page<BlacklistResponse>> getAllBlacklisted(@RequestParam(defaultValue = "0") int page,
                                                                     @RequestParam(defaultValue = "10") int size) {
        Page<Blacklist> blacklists = blacklistService.getAllBlacklisted(PageRequest.of(page, size));
        return ResponseEntity.ok(blacklists.map(this::mapToResponse));
    }

    private BlacklistResponse mapToResponse(Blacklist blacklist) {
        return BlacklistResponse.builder()
                .id(blacklist.getId())
                .mobileNumber(blacklist.getMobileNumber())
                .idNumber(blacklist.getIdNumber())
                .reason(blacklist.getReason())
                .isActive(blacklist.getIsActive())
                .createdAt(blacklist.getCreatedAt())
                .build();
    }
}
