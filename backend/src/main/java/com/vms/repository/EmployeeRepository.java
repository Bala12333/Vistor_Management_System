package com.vms.repository;

import com.vms.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    
    // JOIN FETCH to prevent N+1 queries when loading an Employee and their User/Department
    @Query("SELECT e FROM Employee e JOIN FETCH e.user JOIN FETCH e.department WHERE e.id = :id")
    Optional<Employee> findByIdWithDetails(Long id);
    
    Optional<Employee> findByUserId(Long userId);
}
