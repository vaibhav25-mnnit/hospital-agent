package com.vaibhavs.hospital_agent.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

public record PatientRequest(
        @NotBlank String fullName,
        LocalDate dob,
        String gender,
        String phone) {}