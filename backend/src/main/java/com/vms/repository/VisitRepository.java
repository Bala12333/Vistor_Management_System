package com.vms.repository;

import com.vms.entity.Visit;
import com.vms.entity.enums.VisitStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface VisitRepository extends JpaRepository<Visit, Long> {

    // 🚀 Performance Optimized: JOIN FETCH prevents N+1 queries by fetching relationships in a single query
    @Query("SELECT v FROM Visit v JOIN FETCH v.visitor JOIN FETCH v.employee e JOIN FETCH e.user JOIN FETCH v.category WHERE v.id = :id")
    Optional<Visit> findByIdWithDetails(Long id);

    // Eagerly fetch visitor and employee details for listing
    @Query(value = "SELECT v FROM Visit v JOIN FETCH v.visitor JOIN FETCH v.category",
           countQuery = "SELECT count(v) FROM Visit v")
    Page<Visit> findAllWithDetails(Pageable pageable);

    List<Visit> findByStatus(VisitStatus status);

    // To detect duplicate visits on the same day
    @Query("SELECT v FROM Visit v WHERE v.visitor.mobile = :mobile AND v.status IN (:statuses) AND DATE(v.expectedDate) = DATE(:date)")
    List<Visit> findDuplicateVisits(String mobile, List<VisitStatus> statuses, LocalDateTime date);
}
