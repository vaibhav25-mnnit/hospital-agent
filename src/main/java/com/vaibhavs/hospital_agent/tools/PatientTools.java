package com.vaibhavs.hospital_agent.tools;

import com.vaibhavs.hospital_agent.dto.ToolPatient;
import com.vaibhavs.hospital_agent.dto.ToolReport;
import com.vaibhavs.hospital_agent.dto.ToolReportsResult;
import com.vaibhavs.hospital_agent.service.PatientService;
import com.vaibhavs.hospital_agent.service.ReportService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PatientTools {

    private static final int MAX_RESULTS = 10;

    private final PatientService patientService;
    private final ReportService reportService;

    public PatientTools(PatientService patientService, ReportService reportService) {
        this.patientService = patientService;
        this.reportService = reportService;
    }

    @Tool(description = "Search patients by full or partial name. "
            + "Returns id, name, date of birth,phone and gender for each match. "
            + "Returns an empty list if nobody matches.")
    public List<ToolPatient> findPatients(
            @ToolParam(description = "Full or partial patient name, e.g. 'Rahul Sharma'") String name) {
        System.out.println("*******PATIENT TOOL CALLED**********");
        if (name == null || name.isBlank()) {
            return List.of();
        }
        return patientService.list(name).stream()
                .limit(MAX_RESULTS)
                .map(ToolPatient::from)
                .toList();
    }


    @Tool(description = "Get the medical reports of ONE patient by patient id. "
            + "Returns the patient's name, the report count and the reports. "
            + "Call this every time reports are asked for, even if reports were shown earlier.")
    public ToolReportsResult getReports(
            @ToolParam(description = "The numeric patient id") Long patientId) {
        System.out.println("*******REPORT TOOL CALLED**********");
        var patient = patientService.get(patientId);
        var reports = reportService.listForPatient(patientId).stream()
                .map(ToolReport::from)
                .toList();
        return new ToolReportsResult(ToolPatient.from(patient), reports.size(), reports);
    }
}