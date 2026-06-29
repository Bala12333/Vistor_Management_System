package com.vms.service.impl;

import com.vms.dto.DashboardStatsResponse;
import com.vms.entity.enums.VisitStatus;
import com.vms.repository.VisitRepository;
import com.vms.repository.BlacklistRepository;
import com.vms.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final VisitRepository visitRepository;
    private final BlacklistRepository blacklistRepository;

    @Override
    @Transactional(readOnly = true)
    public DashboardStatsResponse getDashboardStats() {
        // Expected today: any visit with expectedDate today
        LocalDate today = LocalDate.now();
        
        // Let's just fetch all and filter in memory for simplicity unless it's huge
        // In a real system, we'd use native queries.
        long expectedToday = visitRepository.findAll().stream()
                .filter(v -> v.getExpectedDate().toLocalDate().equals(today))
                .count();

        long insidePremises = visitRepository.findByStatus(VisitStatus.CHECKED_IN).size();
        
        long pendingApprovals = visitRepository.findByStatus(VisitStatus.PENDING).size();
        
        long blacklistHits = blacklistRepository.count();

        // 7-day data
        List<String> labels = new ArrayList<>();
        List<Long> data = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE"); // Mon, Tue, etc.

        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            labels.add(date.format(formatter));
            
            long count = visitRepository.findAll().stream()
                    .filter(v -> v.getExpectedDate().toLocalDate().equals(date))
                    .count();
            data.add(count);
        }

        return DashboardStatsResponse.builder()
                .expectedToday(expectedToday)
                .insidePremises(insidePremises)
                .pendingApprovals(pendingApprovals)
                .blacklistHits(blacklistHits)
                .labels(labels)
                .data(data)
                .build();
    }
}
