package com.vaibhavs.hospital_agent.service;

import com.vaibhavs.hospital_agent.dto.PatientRequest;
import com.vaibhavs.hospital_agent.entity.Patient;
import com.vaibhavs.hospital_agent.repository.PatientRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class PatientService {

    private final PatientRepository patients;

    public PatientService(PatientRepository patients) {
        this.patients = patients;
    }

    public List<Patient> list(String name) {
        if (name == null || name.isBlank()) {
            return patients.findAll();
        }
        return patients.findByFullNameContainingIgnoreCase(name.trim());
    }

    public Patient get(Long id) {
        return patients.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient " + id + " not found"));
    }

    @Transactional
    public Patient create(PatientRequest req) {
        Patient p = new Patient();
        apply(p, req);
        return patients.save(p);
    }

    @Transactional
    public Patient update(Long id, PatientRequest req) {
        Patient p = get(id);
        apply(p, req);
        return patients.save(p);
    }

    @Transactional
    public void delete(Long id) {
        patients.delete(get(id));   // DB cascades to the patient's reports
    }

    private void apply(Patient p, PatientRequest req) {
        p.setFullName(req.fullName().trim());
        p.setDob(req.dob());
        p.setGender(req.gender());
        p.setPhone(req.phone());
    }
}