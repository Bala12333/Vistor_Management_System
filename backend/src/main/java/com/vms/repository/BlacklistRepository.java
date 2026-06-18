package com.vms.repository;

import com.vms.entity.Blacklist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BlacklistRepository extends JpaRepository<Blacklist, Long> {

    Optional<Blacklist> findByMobileNumberAndIsActiveTrue(String mobileNumber);

    Optional<Blacklist> findByIdNumberAndIsActiveTrue(String idNumber);
}
