package com.vms.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "check_in_out", indexes = {
        @Index(name = "idx_check_in_out_visit_id", columnList = "visit_id")
})
@Getter
@Setter
public class CheckInOut extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "visit_id", nullable = false, unique = true)
    private Visit visit;

    @Column(name = "check_in_time")
    private LocalDateTime checkInTime;

    @Column(name = "check_out_time")
    private LocalDateTime checkOutTime;
}
