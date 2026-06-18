package com.vms.entity;

import com.vms.entity.enums.ApprovalStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "approvals", indexes = {
        @Index(name = "idx_approvals_visit_id", columnList = "visit_id")
})
@Getter
@Setter
public class Approval extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "visit_id", nullable = false, unique = true)
    private Visit visit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by", nullable = false)
    private User approvedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ApprovalStatus status = ApprovalStatus.PENDING;

    @Column(length = 500)
    private String remarks;
}
