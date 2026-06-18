package com.vms.service;

import com.vms.entity.Visit;
import com.vms.entity.Visitor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface VisitorService {

    Visitor registerVisitor(Visitor visitor);

    Visit scheduleVisit(Long visitorId, Long employeeId, String categoryCode, Visit visitDetails);

    Page<Visit> getActiveVisits(Pageable pageable);

    void processCheckIn(Long visitId);

    void processCheckOut(Long visitId);
}
