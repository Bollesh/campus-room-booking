package com.campusbooking.types; 

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// Using a record for conciseness
public record RegistrationRequest(
    @NotBlank @Email String email,
    @NotBlank @Size(min = 6, message = "Password must be at least 6 characters") String password, // Added password validation
    @NotBlank String name,
    @NotNull Long phone,

    @NotBlank String userType, // e.g., "STUDENT", "PROFESSOR", "FLOOR_MANAGER", "SECURITY", "STUDENT_COUNCIL"

    Long regno,        // For STUDENT, STUDENT_COUNCIL
    Boolean isCultural, // For PROFESSOR
    String position     // For STUDENT_COUNCIL
) {}
