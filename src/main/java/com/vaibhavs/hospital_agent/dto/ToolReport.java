package com.vaibhavs.hospital_agent.dto;

import com.vaibhavs.hospital_agent.entity.Report;
import java.time.LocalDate;

public record ToolReport(Long reportId, Long patientId, String reportType, LocalDate reportDate) {
    public static ToolReport from(Report r) {
        return new ToolReport(r.getId(), r.getPatientId(), r.getReportType(), r.getReportDate());
    }
}