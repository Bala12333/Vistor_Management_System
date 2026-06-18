package com.vms.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

@Entity
@Table(name = "visitors", indexes = {
        @Index(name = "idx_visitors_mobile", columnList = "mobile"),
        @Index(name = "idx_visitors_id_number", columnList = "id_number")
})
@Getter
@Setter
@SQLDelete(sql = "UPDATE visitors SET deleted_at = NOW() WHERE id = ?")
@Where(clause = "deleted_at IS NULL")
public class Visitor extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, length = 20)
    private String mobile;

    @Column(length = 100)
    private String email;

    @Column(length = 150)
    private String company;

    @Column(name = "id_number", length = 50)
    private String idNumber;

    @Column(name = "photo_path", length = 255)
    private String photoPath;
}
