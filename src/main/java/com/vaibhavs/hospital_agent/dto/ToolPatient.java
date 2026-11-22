package com.vaibhavs.hospital_agent.dto;

import com.vaibhavs.hospital_agent.entity.Patient;
import org.stringtemplate.v4.ST;

import java.time.LocalDate;

public record ToolPatient(Long id, String fullName, LocalDate dob, String gender, String phone) {
    public static ToolPatient from(Patient p) {
        return new ToolPatient(p.getId(), p.getFullName(), p.getDob(), p.getGender(), p.getPhone());
    }
}