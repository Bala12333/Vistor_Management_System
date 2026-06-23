package com.vms.controller;

import com.vms.dto.OtpVerificationRequest;
import com.vms.service.SecurityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/security")
@RequiredArgsConstructor
@Tag(name = "Security Operations", description = "Endpoints for OTP verification and blacklist checks")
public class SecurityController {

    private final SecurityService securityService;

    @PostMapping("/otp/send")
    @Operation(summary = "Send an OTP via Twilio to the provided mobile number (Open Endpoint)")
    public ResponseEntity<Void> sendOtp(@RequestParam String mobile) {
        securityService.sendOtp(mobile);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/otp/verify")
    @Operation(summary = "Verify the OTP for a mobile number (Open Endpoint)")
    public ResponseEntity<Boolean> verifyOtp(@Valid @RequestBody OtpVerificationRequest request) {
        boolean isValid = securityService.verifyOtp(request.getMobile(), request.getOtpCode());
        return ResponseEntity.ok(isValid);
    }
}
