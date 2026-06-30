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

    @PostMapping("/otp/send-mobile")
    @Operation(summary = "Send a mock OTP via SMS to the provided mobile number (Open Endpoint)")
    public ResponseEntity<Void> sendMobileOtp(@RequestParam String mobile) {
        securityService.sendMobileOtp(mobile);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/otp/verify-mobile")
    @Operation(summary = "Verify the mock OTP for a mobile number (Open Endpoint)")
    public ResponseEntity<Boolean> verifyMobileOtp(@Valid @RequestBody OtpVerificationRequest request) {
        boolean isValid = securityService.verifyMobileOtp(request.getMobile(), request.getOtpCode());
        return ResponseEntity.ok(isValid);
    }

    @PostMapping("/otp/send-email")
    @Operation(summary = "Send an OTP via Email to the provided email address (Open Endpoint)")
    public ResponseEntity<Void> sendEmailOtp(@RequestParam String email) {
        securityService.sendEmailOtp(email);
        return ResponseEntity.ok().build();
    }


    @PostMapping("/otp/verify-email")
    @Operation(summary = "Verify the OTP for an email address (Open Endpoint)")
    public ResponseEntity<Boolean> verifyEmailOtp(@Valid @RequestBody com.vms.dto.EmailOtpVerificationRequest request) {
        boolean isValid = securityService.verifyEmailOtp(request.getEmail(), request.getOtpCode());
        return ResponseEntity.ok(isValid);
    }
}
