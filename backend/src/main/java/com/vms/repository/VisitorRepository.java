package com.vms.repository;

import com.vms.entity.Visitor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VisitorRepository extends JpaRepository<Visitor, Long> {

    Optional<Visitor> findByMobile(String mobile);

    Optional<Visitor> findByIdNumber(String idNumber);
}
