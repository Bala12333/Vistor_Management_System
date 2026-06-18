package com.vms.service;

public interface SecurityService {

    boolean checkBlacklist(String mobileNumber, String idNumber);

    boolean verifyOtp(String mobileNumber, String otpCode);

    void sendOtp(String mobileNumber);

    String generateQrSignature(Long visitId, Long visitorId, String expectedDate);

    boolean validateQrSignature(String payload);
}
