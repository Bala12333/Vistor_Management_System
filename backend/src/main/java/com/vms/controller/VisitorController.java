package com.vms.controller;

import com.vms.dto.VisitResponse;
import com.vms.dto.VisitorRegistrationRequest;
import com.vms.entity.Visit;
import com.vms.entity.Visitor;
import com.vms.service.VisitorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/visitors")
@RequiredArgsConstructor
@Tag(name = "Visitor Management", description = "Endpoints for visitor registration and tracking")
public class VisitorController {

    private final VisitorService visitorService;

    @PostMapping("/register")
    @Operation(summary = "Register a new visitor and schedule a visit (Open Endpoint)")
    public ResponseEntity<VisitResponse> registerVisitor(@Valid @RequestBody VisitorRegistrationRequest request) {
        // Map DTO to Entity
        Visitor visitor = new Visitor();
        visitor.setName(request.getName());
        visitor.setMobile(request.getMobile());
        visitor.setEmail(request.getEmail());
        visitor.setCompany(request.getCompany());
        visitor.setIdNumber(request.getIdNumber());

        Visitor savedVisitor = visitorService.registerVisitor(visitor);

        Visit visitDetails = new Visit();
        visitDetails.setExpectedDate(request.getExpectedDate());
        visitDetails.setPurpose(request.getPurpose());

        Visit scheduledVisit = visitorService.scheduleVisit(savedVisitor.getId(), request.getEmployeeId(), request.getCategoryCode(), visitDetails);

        return ResponseEntity.ok(mapToResponse(scheduledVisit));
    }

    @GetMapping("/active")
    @Operation(summary = "Get a paginated list of active visits (Secured Endpoint)")
    public ResponseEntity<Page<VisitResponse>> getActiveVisits(@RequestParam(defaultValue = "0") int page,
                                                               @RequestParam(defaultValue = "10") int size) {
        Page<Visit> visits = visitorService.getActiveVisits(PageRequest.of(page, size));
        return ResponseEntity.ok(visits.map(this::mapToResponse));
    }

    @GetMapping("/history")
    @Operation(summary = "Get a paginated list of all visits (History)")
    public ResponseEntity<Page<VisitResponse>> getVisitHistory(@RequestParam(defaultValue = "0") int page,
                                                               @RequestParam(defaultValue = "10") int size) {
        Page<Visit> visits = visitorService.getAllVisits(PageRequest.of(page, size));
        return ResponseEntity.ok(visits.map(this::mapToResponse));
    }

    @GetMapping("/approvals/pending")
    @Operation(summary = "Get a paginated list of visits pending approval")
    public ResponseEntity<Page<VisitResponse>> getPendingApprovals(@RequestParam(defaultValue = "0") int page,
                                                                   @RequestParam(defaultValue = "10") int size) {
        Page<Visit> visits = visitorService.getPendingApprovals(PageRequest.of(page, size));
        return ResponseEntity.ok(visits.map(this::mapToResponse));
    }

    @PostMapping("/{visitId}/approve")
    @Operation(summary = "Approve a pending visit")
    public ResponseEntity<Void> approveVisit(@PathVariable Long visitId) {
        visitorService.approveVisit(visitId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{visitId}/reject")
    @Operation(summary = "Reject a pending visit")
    public ResponseEntity<Void> rejectVisit(@PathVariable Long visitId) {
        visitorService.rejectVisit(visitId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{visitId}/checkin")
    @Operation(summary = "Check-in an approved visitor (Secured Endpoint)")
    public ResponseEntity<Void> checkIn(@PathVariable Long visitId) {
        visitorService.processCheckIn(visitId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{visitId}/checkout")
    @Operation(summary = "Check-out a visitor (Secured Endpoint)")
    public ResponseEntity<Void> checkOut(@PathVariable Long visitId) {
        visitorService.processCheckOut(visitId);
        return ResponseEntity.ok().build();
    }

    private VisitResponse mapToResponse(Visit visit) {
        return VisitResponse.builder()
                .id(visit.getId())
                .visitorName(visit.getVisitor().getName())
                .visitorMobile(visit.getVisitor().getMobile())
                .categoryDisplayName(visit.getCategory().getDisplayName())
                .hostName(visit.getEmployee().getUser().getName())
                .expectedDate(visit.getExpectedDate())
                .status(visit.getStatus())
                .build();
    }
}
