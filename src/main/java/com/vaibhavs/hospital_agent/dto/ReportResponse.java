package com.vaibhavs.hospital_agent.dto;

import com.vaibhavs.hospital_agent.entity.Report;
import java.time.LocalDate;

public record ReportResponse(Long id, Long patientId, String reportType,
                             LocalDate reportDate, String s3Key) {
    public static ReportResponse from(Report r) {
        return new ReportResponse(r.getId(), r.getPatientId(), r.getReportType(),
                r.getReportDate(), r.getS3Key());
    }
}