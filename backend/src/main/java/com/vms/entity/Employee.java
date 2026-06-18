package com.vms.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "employees", indexes = {
        @Index(name = "idx_employees_user_id", columnList = "user_id"),
        @Index(name = "idx_employees_department_id", columnList = "department_id")
})
@Getter
@Setter
public class Employee extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @Column(length = 100)
    private String designation;

    @Column(length = 20)
    private String contact;
}
