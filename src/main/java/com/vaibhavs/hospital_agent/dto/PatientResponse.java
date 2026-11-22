package com.vaibhavs.hospital_agent.dto;

import com.vaibhavs.hospital_agent.entity.Patient;
import java.time.LocalDate;

public record PatientResponse(Long id, String fullName, LocalDate dob,
                              String gender, String phone) {
    public static PatientResponse from(Patient p) {
        return new PatientResponse(p.getId(), p.getFullName(), p.getDob(),
                p.getGender(), p.getPhone());
    }
}