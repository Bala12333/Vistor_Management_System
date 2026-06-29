package com.vms.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class BlacklistRequest {
    private String mobileNumber;
    private String idNumber;
    
    @NotBlank(message = "Reason is mandatory")
    private String reason;
}
