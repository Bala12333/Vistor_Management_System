package com.vms.service;

public interface SecurityService {

    boolean checkBlacklist(String mobileNumber, String idNumber);

    void sendEmailOtp(String email);

    void sendMobileOtp(String mobile);
    boolean verifyMobileOtp(String mobile, String otpCode);
    boolean verifyEmailOtp(String email, String otpCode);

    String generateQrSignature(Long visitId, Long visitorId, String expectedDate);

    boolean validateQrSignature(String payload);
}
