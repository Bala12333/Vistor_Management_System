package com.vms.service.impl;

import com.vms.entity.*;
import com.vms.entity.enums.VisitStatus;
import com.vms.repository.*;
import com.vms.service.BlacklistService;
import com.vms.service.VisitorService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VisitorServiceImpl implements VisitorService {

    private final VisitorRepository visitorRepository;
    private final VisitRepository visitRepository;
    private final EmployeeRepository employeeRepository;
    private final VisitorCategoryRepository visitorCategoryRepository;
    private final CheckInOutRepository checkInOutRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final BlacklistService blacklistService;

    @Override
    @Transactional
    public Visitor registerVisitor(Visitor visitor) {
        if (blacklistService.isBlacklisted(visitor.getMobile(), visitor.getIdNumber())) {
            throw new RuntimeException("Visitor is blacklisted and cannot be registered.");
        }
        return visitorRepository.findByMobile(visitor.getMobile())
                .orElseGet(() -> visitorRepository.save(visitor));
    }

    @Override
    @Transactional
    public Visit scheduleVisit(Long visitorId, Long employeeId, String categoryCode, Visit visitDetails) {
        Visitor visitor = visitorRepository.findById(visitorId)
                .orElseThrow(() -> new RuntimeException("Visitor not found"));
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found"));
        VisitorCategory category = visitorCategoryRepository.findById(categoryCode)
                .orElseThrow(() -> new RuntimeException("Category not found"));

        visitDetails.setVisitor(visitor);
        visitDetails.setEmployee(employee);
        visitDetails.setCategory(category);
        
        if (category.getRequiresApproval()) {
            visitDetails.setStatus(VisitStatus.PENDING);
            // Trigger approval workflow via Webhook (Day 17)
        } else {
            visitDetails.setStatus(VisitStatus.APPROVED);
        }

        Visit savedVisit = visitRepository.save(visitDetails);
        messagingTemplate.convertAndSend("/topic/visits", "NEW_VISIT");
        return savedVisit;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Visit> getActiveVisits(Pageable pageable) {
        return visitRepository.findByStatusInWithDetails(
            List.of(VisitStatus.APPROVED, VisitStatus.CHECKED_IN), pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Visit> getAllVisits(Pageable pageable) {
        return visitRepository.findAllWithDetails(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Visit> getPendingApprovals(Pageable pageable) {
        return visitRepository.findByStatusInWithDetails(
            List.of(VisitStatus.PENDING), pageable);
    }

    @Override
    @Transactional
    public void approveVisit(Long visitId) {
        Visit visit = visitRepository.findById(visitId)
                .orElseThrow(() -> new RuntimeException("Visit not found"));
        if (visit.getStatus() != VisitStatus.PENDING) {
            throw new RuntimeException("Visit is not pending approval");
        }
        visit.setStatus(VisitStatus.APPROVED);
        visitRepository.save(visit);
        messagingTemplate.convertAndSend("/topic/visits", "STATUS_UPDATE");
    }

    @Override
    @Transactional
    public void rejectVisit(Long visitId) {
        Visit visit = visitRepository.findById(visitId)
                .orElseThrow(() -> new RuntimeException("Visit not found"));
        if (visit.getStatus() != VisitStatus.PENDING) {
            throw new RuntimeException("Visit is not pending approval");
        }
        visit.setStatus(VisitStatus.REJECTED);
        visitRepository.save(visit);
        messagingTemplate.convertAndSend("/topic/visits", "STATUS_UPDATE");
    }

    @Override
    @Transactional
    public void processCheckIn(Long visitId) {
        Visit visit = visitRepository.findByIdWithDetails(visitId)
                .orElseThrow(() -> new RuntimeException("Visit not found"));

        if (visit.getStatus() != VisitStatus.APPROVED) {
            throw new RuntimeException("Visit is not approved for check-in");
        }

        visit.setStatus(VisitStatus.CHECKED_IN);
        visitRepository.save(visit);

        CheckInOut checkInOut = new CheckInOut();
        checkInOut.setVisit(visit);
        checkInOut.setCheckInTime(LocalDateTime.now());
        checkInOutRepository.save(checkInOut);
        
        messagingTemplate.convertAndSend("/topic/visits", "CHECK_IN");
    }

    @Override
    @Transactional
    public void processCheckOut(Long visitId) {
        Visit visit = visitRepository.findById(visitId)
                .orElseThrow(() -> new RuntimeException("Visit not found"));

        if (visit.getStatus() != VisitStatus.CHECKED_IN) {
            throw new RuntimeException("Visitor has not checked in");
        }

        visit.setStatus(VisitStatus.COMPLETED);
        visitRepository.save(visit);

        CheckInOut checkInOut = checkInOutRepository.findByVisitId(visitId)
                .orElseThrow(() -> new RuntimeException("Check-in record not found"));
        checkInOut.setCheckOutTime(LocalDateTime.now());
        checkInOutRepository.save(checkInOut);
        
        messagingTemplate.convertAndSend("/topic/visits", "CHECK_OUT");
    }
}
