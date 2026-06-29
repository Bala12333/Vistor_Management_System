package com.vms.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class DashboardStatsResponse {
    private long expectedToday;
    private long insidePremises;
    private long pendingApprovals;
    private long blacklistHits;
    
    // Data for the 7-day chart
    private List<String> labels; // e.g., "Mon", "Tue", etc.
    private List<Long> data;     // Visitor counts for those days
}
