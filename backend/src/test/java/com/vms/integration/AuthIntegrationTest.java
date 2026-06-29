package com.vms.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vms.dto.AuthRequest;
import com.vms.entity.User;
import com.vms.entity.enums.Role;
import com.vms.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setup() {
        if (userRepository.findByEmail("test@vms.com").isEmpty()) {
            User user = new User();
            user.setName("Test User");
            user.setEmail("test@vms.com");
            user.setPasswordHash(passwordEncoder.encode("Test@123"));
            user.setRole(Role.ADMIN);
            userRepository.save(user);
        }
    }

    @Test
    void testSuccessfulLogin() throws Exception {
        AuthRequest request = new AuthRequest();
        request.setEmail("test@vms.com");
        request.setPassword("Test@123");

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.email").value("test@vms.com"));
    }

    @Test
    void testFailedLoginWithBadPassword() throws Exception {
        AuthRequest request = new AuthRequest();
        request.setEmail("test@vms.com");
        request.setPassword("WrongPassword");

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testFailedLoginWithUnknownUser() throws Exception {
        AuthRequest request = new AuthRequest();
        request.setEmail("unknown@vms.com");
        request.setPassword("Test@123");

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
}
