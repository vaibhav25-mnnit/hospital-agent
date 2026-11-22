package com.vaibhavs.hospital_agent.controllers;

import com.vaibhavs.hospital_agent.dto.PatientRequest;
import com.vaibhavs.hospital_agent.dto.PatientResponse;
import com.vaibhavs.hospital_agent.service.PatientService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/patients")
public class PatientController {

    private final PatientService service;

    public PatientController(PatientService service) {
        this.service = service;
    }

    @GetMapping
    public List<PatientResponse> list(@RequestParam(required = false) String name) {
        return service.list(name).stream().map(PatientResponse::from).toList();
    }

    @GetMapping("/{id}")
    public PatientResponse get(@PathVariable Long id) {
        return PatientResponse.from(service.get(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PatientResponse create(@Valid @RequestBody PatientRequest req) {
        return PatientResponse.from(service.create(req));
    }

    @PutMapping("/{id}")
    public PatientResponse update(@PathVariable Long id, @Valid @RequestBody PatientRequest req) {
        return PatientResponse.from(service.update(id, req));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}