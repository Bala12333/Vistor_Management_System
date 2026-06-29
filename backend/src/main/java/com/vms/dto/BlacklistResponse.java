package com.vms.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class BlacklistResponse {
    private Long id;
    private String mobileNumber;
    private String idNumber;
    private String reason;
    private Boolean isActive;
    private LocalDateTime createdAt;
}
