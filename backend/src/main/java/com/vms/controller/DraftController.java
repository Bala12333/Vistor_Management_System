package com.vms.controller;

import com.vms.dto.RegistrationDraftDto;
import com.vms.service.DraftService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/draft")
@RequiredArgsConstructor
public class DraftController {

    private final DraftService draftService;
    private static final String COOKIE_NAME = "vms_draft_session";

    @PostMapping
    public ResponseEntity<Void> saveDraft(
            @CookieValue(value = COOKIE_NAME, required = false) String sessionId,
            @RequestBody RegistrationDraftDto draftDto,
            HttpServletResponse response) {
        
        if (sessionId == null) {
            sessionId = UUID.randomUUID().toString();
            Cookie cookie = new Cookie(COOKIE_NAME, sessionId);
            cookie.setHttpOnly(true);
            cookie.setSecure(true);
            cookie.setPath("/");
            cookie.setMaxAge(24 * 60 * 60); // 24 hours
            cookie.setAttribute("SameSite", "Strict");
            response.addCookie(cookie);
        }

        draftService.saveDraft(sessionId, draftDto);
        return ResponseEntity.ok().build();
    }

    @GetMapping
    public ResponseEntity<RegistrationDraftDto> getDraft(
            @CookieValue(value = COOKIE_NAME, required = false) String sessionId) {
        
        if (sessionId == null) {
            return ResponseEntity.noContent().build();
        }

        Optional<RegistrationDraftDto> draft = draftService.getDraft(sessionId);
        return draft.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteDraft(
            @CookieValue(value = COOKIE_NAME, required = false) String sessionId,
            HttpServletResponse response) {
        
        if (sessionId != null) {
            draftService.deleteDraft(sessionId);
            
            // Clear cookie
            Cookie cookie = new Cookie(COOKIE_NAME, "");
            cookie.setHttpOnly(true);
            cookie.setSecure(true);
            cookie.setPath("/");
            cookie.setMaxAge(0);
            cookie.setAttribute("SameSite", "Strict");
            response.addCookie(cookie);
        }
        
        return ResponseEntity.ok().build();
    }
}
