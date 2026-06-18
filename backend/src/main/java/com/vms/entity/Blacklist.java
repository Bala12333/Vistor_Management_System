package com.vms.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "blacklist", indexes = {
        @Index(name = "idx_blacklist_mobile", columnList = "mobile_number"),
        @Index(name = "idx_blacklist_id_number", columnList = "id_number")
})
@Getter
@Setter
public class Blacklist extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "mobile_number", length = 20)
    private String mobileNumber;

    @Column(name = "id_number", length = 50)
    private String idNumber;

    @Column(nullable = false, length = 500)
    private String reason;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
}
