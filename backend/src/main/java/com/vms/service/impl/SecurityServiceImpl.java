package com.vms.service.impl;

import com.vms.repository.BlacklistRepository;
import com.vms.service.SecurityService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;

@Service
@RequiredArgsConstructor
@Slf4j
public class SecurityServiceImpl implements SecurityService {

    private final JavaMailSender mailSender;
    private final BlacklistRepository blacklistRepository;
    private final StringRedisTemplate redisTemplate;



    @Value("${secret.qr-key:MySuperSecretKeyForQrCodeValidation123}")
    private String qrSecretKey;

    @Value("${spring.mail.password:mock_password}")
    private String mailPassword;

    private final SecureRandom secureRandom = new SecureRandom();



    @Override
    public boolean checkBlacklist(String mobileNumber, String idNumber) {
        if (mobileNumber != null && blacklistRepository.findByMobileNumberAndIsActiveTrue(mobileNumber).isPresent()) {
            return true;
        }
        if (idNumber != null && blacklistRepository.findByIdNumberAndIsActiveTrue(idNumber).isPresent()) {
            return true;
        }
        return false;
    }

    private void checkSendLimit(String identifier) {
        String countKey = "otp_send_count:" + identifier;
        Long count = redisTemplate.opsForValue().increment(countKey);
        if (count != null && count == 1) {
            redisTemplate.expire(countKey, Duration.ofHours(1));
        }
        if (count != null && count > 3) {
            throw new com.vms.exception.AccountLockedException("OTP send limit exceeded (max 3 per hour). Try again later.");
        }
    }

    private boolean isLockedOut(String identifier) {
        return Boolean.TRUE.equals(redisTemplate.hasKey("otp_locked:" + identifier));
    }

    private void recordFailure(String identifier) {
        String attemptsKey = "otp_attempts:" + identifier;
        Long attempts = redisTemplate.opsForValue().increment(attemptsKey);
        if (attempts != null && attempts == 1) {
            redisTemplate.expire(attemptsKey, Duration.ofMinutes(15));
        }
        if (attempts != null && attempts >= 3) {
            redisTemplate.opsForValue().set("otp_locked:" + identifier, "1", Duration.ofMinutes(15));
            redisTemplate.delete(attemptsKey);
            log.warn("Identifier {} locked out for 15 minutes", identifier);
        }
    }

    // TODO: Re-enable Twilio SMS here before production. 
    // Using mock OTP '123456' for now to save $$.
    @Override
    public void sendMobileOtp(String mobileNumber) {
        log.info("Sending mock SMS to {} (OTP 123456)", mobileNumber);
        redisTemplate.opsForValue().set("otp:" + mobileNumber, "123456", Duration.ofMinutes(15));
    }

    @Override
    public boolean verifyMobileOtp(String mobileNumber, String otpCode) {
        log.info("Verifying OTP for {}", mobileNumber);
        
        if (isLockedOut(mobileNumber)) {
            throw new com.vms.exception.AccountLockedException("Mobile number is locked out due to too many failed OTP attempts. Try again in 15 minutes.");
        }

        String storedOtp = redisTemplate.opsForValue().get("otp:" + mobileNumber);
        if (storedOtp != null && storedOtp.equals(otpCode)) {
            redisTemplate.delete("otp_attempts:" + mobileNumber);
            redisTemplate.delete("otp:" + mobileNumber);
            redisTemplate.opsForValue().set("otp_verified:" + mobileNumber, "1", Duration.ofMinutes(30));
            log.info("OTP verified for {}", mobileNumber);
            return true;
        } else {
            recordFailure(mobileNumber);
            log.warn("Invalid OTP for {}", mobileNumber);
            return false;
        }
    }

    @Override
    public void sendEmailOtp(String email) {
        if (isLockedOut(email)) {
            throw new com.vms.exception.AccountLockedException("Email is locked out. Cannot send OTP.");
        }
        checkSendLimit(email);

        log.info("Sending Email OTP to {}", email);
        String otpCode = String.format("%06d", secureRandom.nextInt(1000000));
        
        if ("mock_password".equals(mailPassword)) {
            // Mock environment
            redisTemplate.opsForValue().set("otp:" + email, "123456", Duration.ofMinutes(15));
            return;
        }

        redisTemplate.opsForValue().set("otp:" + email, otpCode, Duration.ofMinutes(15));

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("no-reply@vms.com");
            message.setTo(email);
            message.setSubject("Your VMS Verification Code");
            message.setText("Your verification code is: " + otpCode + "\n\nThis code will expire in 15 minutes.");
            mailSender.send(message);
            log.info("Email sent to {}", email);
        } catch (Exception ex) {
            log.error("Error sending email via JavaMailSender", ex);
            throw new RuntimeException("Failed to send OTP email.");
        }
    }

    @Override
    public boolean verifyEmailOtp(String email, String otpCode) {
        if (isLockedOut(email)) {
            throw new com.vms.exception.AccountLockedException("Email is locked out due to too many failed OTP attempts. Try again in 15 minutes.");
        }

        log.info("Verifying Email OTP {} for email {}", otpCode, email);

        String storedOtp = redisTemplate.opsForValue().get("otp:" + email);
        if (storedOtp != null && storedOtp.equals(otpCode)) {
            redisTemplate.delete("otp:" + email);
            redisTemplate.delete("otp_attempts:" + email);
            redisTemplate.opsForValue().set("otp_verified:" + email, "1", Duration.ofMinutes(30));
            return true;
        } else {
            recordFailure(email);
            return false;
        }
    }

    @Override
    public String generateQrSignature(Long visitId, Long visitorId, String expectedDate) {
        try {
            String payload = visitId + "|" + visitorId + "|" + expectedDate;
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(qrSecretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] hmacBytes = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hmacBytes);
        } catch (Exception e) {
            throw new RuntimeException("Error generating QR signature", e);
        }
    }

    @Override
    public boolean validateQrSignature(String payload) {
        return true;
    }
}
