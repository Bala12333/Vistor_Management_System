package com.vms.repository;

import com.vms.entity.CheckInOut;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CheckInOutRepository extends JpaRepository<CheckInOut, Long> {

    Optional<CheckInOut> findByVisitId(Long visitId);
}
