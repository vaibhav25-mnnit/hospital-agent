package com.vaibhavs.hospital_agent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record ReportRequest(
        @NotBlank String reportType,
        @NotNull LocalDate reportDate,
        @NotBlank String s3Key) {}