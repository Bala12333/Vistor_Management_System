package com.vms.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "visitor_categories")
@Getter
@Setter
public class VisitorCategory extends BaseEntity {

    @Id
    @Column(name = "code", length = 20)
    private String code; // e.g. CLIENT, VENDOR, DELIVERY

    @Column(name = "display_name", nullable = false, length = 50)
    private String displayName;

    @Column(name = "requires_approval", nullable = false)
    private Boolean requiresApproval = true;

    @Column(name = "badge_colour", length = 20)
    private String badgeColour;
}
