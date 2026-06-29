package com.vms.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vms.dto.VisitorRegistrationRequest;
import com.vms.entity.VisitorCategory;
import com.vms.repository.VisitorCategoryRepository;
import com.vms.repository.UserRepository;
import com.vms.repository.EmployeeRepository;
import com.vms.entity.User;
import com.vms.entity.Employee;
import com.vms.entity.Department;
import com.vms.entity.enums.Role;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityManager;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class VisitorIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private VisitorCategoryRepository visitorCategoryRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Long testEmployeeId;

    @BeforeEach
    void setup() {
        if (!visitorCategoryRepository.existsById("CLIENT")) {
            VisitorCategory cat = new VisitorCategory();
            cat.setCode("CLIENT");
            cat.setDisplayName("Client");
            cat.setRequiresApproval(true);
            visitorCategoryRepository.save(cat);
        }

        if (employeeRepository.count() == 0) {
            Department dept = new Department();
            dept.setDepartmentName("Test Department");
            entityManager.persist(dept);

            User user = new User();
            user.setName("Host User");
            user.setEmail("host@vms.com");
            user.setPasswordHash(passwordEncoder.encode("Test@123"));
            user.setRole(Role.EMPLOYEE);
            user = userRepository.save(user);

            Employee emp = new Employee();
            emp.setUser(user);
            emp.setDepartment(dept);
            emp.setContact("1234567890");
            emp = employeeRepository.save(emp);
            testEmployeeId = emp.getId();
        } else {
            testEmployeeId = employeeRepository.findAll().get(0).getId();
        }
    }

    @Test
    void testSuccessfulVisitorRegistration() throws Exception {
        VisitorRegistrationRequest request = new VisitorRegistrationRequest();
        request.setName("John Doe Test");
        request.setEmail("johndoe@test.com");
        request.setMobile("+919999988888");
        request.setCategoryCode("CLIENT");
        request.setEmployeeId(testEmployeeId);
        request.setIdNumber("ID12345");
        request.setExpectedDate(LocalDateTime.parse("2026-10-10T10:00:00"));
        request.setPurpose("Business Meeting");

        // The mock OTP logic assumes phone is verified. If our controller doesn't block unverified tests yet, it will pass
        // Or we might get a 200 OK.
        mockMvc.perform(post("/api/v1/visitors/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void testRegistrationFailsWithInvalidMobile() throws Exception {
        VisitorRegistrationRequest request = new VisitorRegistrationRequest();
        request.setName("John Doe Test");
        request.setEmail("johndoe@test.com");
        request.setMobile("123"); // Invalid mobile length
        request.setCategoryCode("CLIENT");
        request.setExpectedDate(LocalDateTime.parse("2026-10-10T10:00:00"));
        request.setPurpose("Business Meeting");

        mockMvc.perform(post("/api/v1/visitors/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
