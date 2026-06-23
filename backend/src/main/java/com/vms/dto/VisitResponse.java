package com.vms.dto;

import com.vms.entity.enums.VisitStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class VisitResponse {
    
    private Long id;
    private String visitorName;
    private String visitorMobile;
    private String categoryDisplayName;
    private String hostName;
    private LocalDateTime expectedDate;
    private VisitStatus status;
}
