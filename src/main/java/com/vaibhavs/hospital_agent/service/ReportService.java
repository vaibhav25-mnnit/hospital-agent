package com.vaibhavs.hospital_agent.service;

import com.vaibhavs.hospital_agent.dto.ReportRequest;
import com.vaibhavs.hospital_agent.entity.Report;
import com.vaibhavs.hospital_agent.repository.PatientRepository;
import com.vaibhavs.hospital_agent.repository.ReportRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class ReportService {

    private final ReportRepository reports;
    private final PatientRepository patients;

    public ReportService(ReportRepository reports, PatientRepository patients) {
        this.reports = reports;
        this.patients = patients;
    }

    public List<Report> listForPatient(Long patientId) {
        requirePatient(patientId);
        return reports.findByPatientId(patientId);
    }

    public Report get(Long id) {
        return reports.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Report " + id + " not found"));
    }

    @Transactional
    public Report create(Long patientId, ReportRequest req) {
        requirePatient(patientId);
        Report r = new Report();
        r.setPatientId(patientId);
        apply(r, req);
        return reports.save(r);
    }

    @Transactional
    public Report update(Long id, ReportRequest req) {
        Report r = get(id);
        apply(r, req);
        return reports.save(r);
    }

    @Transactional
    public void delete(Long id) {
        reports.delete(get(id));
    }

    private void requirePatient(Long patientId) {
        if (!patients.existsById(patientId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient " + patientId + " not found");
        }
    }

    private void apply(Report r, ReportRequest req) {
        r.setReportType(req.reportType().trim());
        r.setReportDate(req.reportDate());
        r.setS3Key(req.s3Key().trim());
    }
}