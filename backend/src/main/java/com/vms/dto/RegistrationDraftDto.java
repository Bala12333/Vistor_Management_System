package com.vms.dto;

import lombok.Data;

@Data
public class RegistrationDraftDto {
    private String name;
    private String mobile;
    private String email;
    private String purpose;
    private String categoryCode;
    private Long hostId;
    private String expectedDate;
    
    // Explicitly excluding file paths for security as per FR-DRAFT-05
}
