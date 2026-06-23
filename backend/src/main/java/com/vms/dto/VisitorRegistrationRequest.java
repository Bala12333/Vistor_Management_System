package com.vms.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class VisitorRegistrationRequest {

    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 150, message = "Name must be between 2 and 150 characters")
    private String name;

    @NotBlank(message = "Mobile number is required")
    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Invalid mobile number. Must match E.164 format.")
    private String mobile;

    @Email(message = "Invalid email format")
    private String email;

    @Size(max = 150, message = "Company name too long")
    private String company;

    @NotBlank(message = "ID Number is required")
    @Size(min = 4, max = 50, message = "Invalid ID Number length")
    private String idNumber;

    @NotNull(message = "Host Employee ID is required")
    private Long employeeId;

    @NotBlank(message = "Category Code is required")
    private String categoryCode;

    @NotNull(message = "Expected visit date is required")
    @FutureOrPresent(message = "Visit date cannot be in the past")
    private LocalDateTime expectedDate;

    @Size(max = 255, message = "Purpose description too long")
    private String purpose;
}
