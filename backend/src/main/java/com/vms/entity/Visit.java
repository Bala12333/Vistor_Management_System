package com.vms.entity;

import com.vms.entity.enums.VisitStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "visits", indexes = {
        @Index(name = "idx_visits_visitor_id", columnList = "visitor_id"),
        @Index(name = "idx_visits_employee_id", columnList = "employee_id"),
        @Index(name = "idx_visits_status", columnList = "status"),
        @Index(name = "idx_visits_expected_date", columnList = "expected_date")
})
@Getter
@Setter
public class Visit extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "visitor_id", nullable = false)
    private Visitor visitor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_code", nullable = false)
    private VisitorCategory category;

    @Column(name = "expected_date", nullable = false)
    private LocalDateTime expectedDate;

    @Column(length = 255)
    private String purpose;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private VisitStatus status = VisitStatus.PENDING;

    @Column(name = "qr_payload", columnDefinition = "TEXT")
    private String qrPayload;
}
