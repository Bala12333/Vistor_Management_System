package com.vms.config;

import com.vms.entity.*;
import com.vms.entity.enums.Role;
import com.vms.entity.enums.VisitStatus;
import com.vms.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final EmployeeRepository employeeRepository;
    private final VisitorCategoryRepository visitorCategoryRepository;
    private final VisitorRepository visitorRepository;
    private final VisitRepository visitRepository;
    private final CheckInOutRepository checkInOutRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        log.info("--- Starting Database Seeding ---");

        // 1. Users
        if (userRepository.findByEmail("admin@vms.com").isEmpty()) {
            User admin = new User();
            admin.setName("System Admin");
            admin.setEmail("admin@vms.com");
            admin.setPasswordHash(passwordEncoder.encode("Admin@123"));
            admin.setRole(Role.ADMIN);
            userRepository.save(admin);
            log.info("Default Admin created: admin@vms.com / Admin@123");
        }

        if (userRepository.findByEmail("reception@vms.com").isEmpty()) {
            User reception = new User();
            reception.setName("Front Desk Reception");
            reception.setEmail("reception@vms.com");
            reception.setPasswordHash(passwordEncoder.encode("Reception@123"));
            reception.setRole(Role.RECEPTION);
            userRepository.save(reception);
            log.info("Default Reception created: reception@vms.com / Reception@123");
        }

        User employeeUser = userRepository.findByEmail("employee@vms.com").orElse(null);
        if (employeeUser == null) {
            employeeUser = new User();
            employeeUser.setName("John Employee");
            employeeUser.setEmail("employee@vms.com");
            employeeUser.setPasswordHash(passwordEncoder.encode("Employee@123"));
            employeeUser.setRole(Role.EMPLOYEE);
            employeeUser = userRepository.save(employeeUser);
            log.info("Default Employee created: employee@vms.com / Employee@123");
        }

        // 2. Departments
        if (departmentRepository.count() == 0) {
            Department it = new Department();
            it.setDepartmentName("IT");
            departmentRepository.save(it);

            Department hr = new Department();
            hr.setDepartmentName("Human Resources");
            departmentRepository.save(hr);
            log.info("Departments IT and HR created.");
        }

        Department itDept = departmentRepository.findAll().stream()
                .filter(d -> d.getDepartmentName().equals("IT"))
                .findFirst()
                .orElse(null);

        // 3. Employee mapping
        Employee hostEmployee = null;
        if (employeeRepository.count() == 0 && itDept != null && employeeUser != null) {
            hostEmployee = new Employee();
            hostEmployee.setUser(employeeUser);
            hostEmployee.setDepartment(itDept);
            hostEmployee.setDesignation("Software Engineer");
            hostEmployee.setContact("555-0101");
            hostEmployee = employeeRepository.save(hostEmployee);
            log.info("Mapped Employee user to IT department");
        } else {
            hostEmployee = employeeRepository.findAll().stream().findFirst().orElse(null);
        }

        // 4. Categories
        if (visitorCategoryRepository.count() == 0) {
            VisitorCategory client = new VisitorCategory();
            client.setCode("CLIENT");
            client.setDisplayName("Client Meeting");
            client.setBadgeColour("blue");
            visitorCategoryRepository.save(client);

            VisitorCategory vendor = new VisitorCategory();
            vendor.setCode("VENDOR");
            vendor.setDisplayName("Vendor / Supplier");
            vendor.setBadgeColour("orange");
            visitorCategoryRepository.save(vendor);
            log.info("Visitor categories CLIENT and VENDOR created.");
        }

        if (visitorCategoryRepository.findById("INTERVIEW").isEmpty()) {
            VisitorCategory interview = new VisitorCategory();
            interview.setCode("INTERVIEW");
            interview.setDisplayName("Interview Candidate");
            interview.setBadgeColour("purple");
            visitorCategoryRepository.save(interview);
            
            VisitorCategory delivery = new VisitorCategory();
            delivery.setCode("DELIVERY");
            delivery.setDisplayName("Delivery Personnel");
            delivery.setBadgeColour("yellow");
            visitorCategoryRepository.save(delivery);
            
            VisitorCategory service = new VisitorCategory();
            service.setCode("SERVICE");
            service.setDisplayName("Service / Tech");
            service.setBadgeColour("gray");
            visitorCategoryRepository.save(service);
            
            VisitorCategory guest = new VisitorCategory();
            guest.setCode("GUEST");
            guest.setDisplayName("Personal Guest");
            guest.setBadgeColour("green");
            visitorCategoryRepository.save(guest);
            
            log.info("Missing visitor categories added.");
        }

        VisitorCategory clientCategory = visitorCategoryRepository.findById("CLIENT").orElse(null);

        // 5. Visitors and Visits
        if (visitRepository.count() == 0 && hostEmployee != null && clientCategory != null) {
            log.info("Seeding 5 sample visitors and visits...");
            String[] names = { "Alice Smith", "Bob Jones", "Charlie Brown", "Diana Prince", "Eve Adams" };
            String[] phones = { "9876543210", "9876543211", "9876543212", "9876543213", "9876543214" };
            VisitStatus[] statuses = {
                    VisitStatus.CHECKED_IN,
                    VisitStatus.PENDING,
                    VisitStatus.CHECKED_IN,
                    VisitStatus.COMPLETED,
                    VisitStatus.CHECKED_IN
            };

            for (int i = 0; i < 5; i++) {
                Visitor v = new Visitor();
                v.setName(names[i]);
                v.setMobile(phones[i]);
                v.setCompany("Test Corp " + i);
                v = visitorRepository.save(v);

                Visit visit = new Visit();
                visit.setVisitor(v);
                visit.setEmployee(hostEmployee);
                visit.setCategory(clientCategory);
                visit.setExpectedDate(LocalDateTime.now().minusHours(i)); // Today
                visit.setPurpose("Meeting " + i);
                visit.setStatus(statuses[i]);
                visitRepository.save(visit);

                if (statuses[i] == VisitStatus.CHECKED_IN || statuses[i] == VisitStatus.COMPLETED) {
                    CheckInOut cio = new CheckInOut();
                    cio.setVisit(visit);
                    cio.setCheckInTime(LocalDateTime.now().minusMinutes(30));
                    if (statuses[i] == VisitStatus.COMPLETED) {
                        cio.setCheckOutTime(LocalDateTime.now().minusMinutes(5));
                    }
                    checkInOutRepository.save(cio);
                }
            }
            log.info("5 sample visitors added successfully.");
        }

        log.info("--- Database Seeding Complete ---");
    }
}
