package com.vaibhavs.hospital_agent.controllers;

import com.vaibhavs.hospital_agent.dto.ReportRequest;
import com.vaibhavs.hospital_agent.dto.ReportResponse;
import com.vaibhavs.hospital_agent.service.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ReportController {

    private final ReportService service;

    public ReportController(ReportService service) {
        this.service = service;
    }

    @GetMapping("/patients/{patientId}/reports")
    public List<ReportResponse> listForPatient(@PathVariable Long patientId) {
        return service.listForPatient(patientId).stream().map(ReportResponse::from).toList();
    }

    @PostMapping("/patients/{patientId}/reports")
    @ResponseStatus(HttpStatus.CREATED)
    public ReportResponse create(@PathVariable Long patientId,
                                 @Valid @RequestBody ReportRequest req) {
        return ReportResponse.from(service.create(patientId, req));
    }

    @GetMapping("/reports/{id}")
    public ReportResponse get(@PathVariable Long id) {
        return ReportResponse.from(service.get(id));
    }

    @PutMapping("/reports/{id}")
    public ReportResponse update(@PathVariable Long id, @Valid @RequestBody ReportRequest req) {
        return ReportResponse.from(service.update(id, req));
    }

    @DeleteMapping("/reports/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}