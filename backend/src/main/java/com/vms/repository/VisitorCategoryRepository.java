package com.vms.repository;

import com.vms.entity.VisitorCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VisitorCategoryRepository extends JpaRepository<VisitorCategory, String> {
}
