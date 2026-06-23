import com.vms.repository.BlacklistRepository;
import com.vms.service.SecurityService;
import com.twilio.Twilio;
import com.twilio.rest.verify.v2.service.Verification;
import com.twilio.rest.verify.v2.service.VerificationCheck;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class SecurityServiceImpl implements SecurityService {

    private final BlacklistRepository blacklistRepository;

    @Value("${twilio.account-sid:dummy_sid}")
    private String twilioAccountSid;

    @Value("${twilio.auth-token:dummy_token}")
    private String twilioAuthToken;

    @Value("${twilio.verify-service-sid:dummy_service_sid}")
    private String verifyServiceSid;

    @Value("${secret.qr-key:MySuperSecretKeyForQrCodeValidation123}")
    private String qrSecretKey;

    // Mobile Number -> Fail Count
    private final Map<String, Integer> otpFailures = new ConcurrentHashMap<>();
    // Mobile Number -> Lock Expiration Time
    private final Map<String, LocalDateTime> lockouts = new ConcurrentHashMap<>();

    @PostConstruct
    public void initTwilio() {
        if (!"dummy_sid".equals(twilioAccountSid)) {
            Twilio.init(twilioAccountSid, twilioAuthToken);
        }
    }

    @Override
    public boolean checkBlacklist(String mobileNumber, String idNumber) {
        if (mobileNumber != null && blacklistRepository.findByMobileNumberAndIsActiveTrue(mobileNumber).isPresent()) {
            return true; // Hit
        }
        if (idNumber != null && blacklistRepository.findByIdNumberAndIsActiveTrue(idNumber).isPresent()) {
            return true; // Hit
        }
        return false;
    }

    @Override
    public boolean verifyOtp(String mobileNumber, String otpCode) {
        if (isLockedOut(mobileNumber)) {
            throw new RuntimeException("Mobile number is locked out due to too many failed OTP attempts. Try again in 15 minutes.");
        }

        log.info("Verifying OTP {} for mobile {}", otpCode, mobileNumber);
        
        if ("dummy_sid".equals(twilioAccountSid)) {
            // Mock environment, assume 123456 is the valid OTP
            if ("123456".equals(otpCode)) {
                otpFailures.remove(mobileNumber);
                return true;
            } else {
                recordFailure(mobileNumber);
                return false;
            }
        }

        try {
            VerificationCheck verificationCheck = VerificationCheck.creator(
                    verifyServiceSid)
                    .setTo(mobileNumber)
                    .setCode(otpCode)
                    .create();

            if ("approved".equals(verificationCheck.getStatus())) {
                otpFailures.remove(mobileNumber);
                return true;
            } else {
                recordFailure(mobileNumber);
                return false;
            }
        } catch (Exception e) {
            log.error("Twilio Verification error", e);
            recordFailure(mobileNumber);
            return false;
        }
    }

    private void recordFailure(String mobileNumber) {
        int failures = otpFailures.getOrDefault(mobileNumber, 0) + 1;
        if (failures >= 3) {
            lockouts.put(mobileNumber, LocalDateTime.now().plusMinutes(15));
            otpFailures.remove(mobileNumber);
            log.warn("Mobile number {} locked out for 15 minutes", mobileNumber);
        } else {
            otpFailures.put(mobileNumber, failures);
        }
    }

    private boolean isLockedOut(String mobileNumber) {
        LocalDateTime lockoutTime = lockouts.get(mobileNumber);
        if (lockoutTime != null) {
            if (LocalDateTime.now().isBefore(lockoutTime)) {
                return true;
            } else {
                lockouts.remove(mobileNumber);
            }
        }
        return false;
    }

    @Override
    public void sendOtp(String mobileNumber) {
        if (isLockedOut(mobileNumber)) {
            throw new RuntimeException("Mobile number is locked out. Cannot send OTP.");
        }

        log.info("Sending OTP to {}", mobileNumber);
        if (!"dummy_sid".equals(twilioAccountSid)) {
            Verification.creator(
                    verifyServiceSid,
                    mobileNumber,
                    "sms")
                    .create();
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
