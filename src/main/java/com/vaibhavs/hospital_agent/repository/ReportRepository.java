package com.vaibhavs.hospital_agent.repository;

import com.vaibhavs.hospital_agent.entity.Report;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ReportRepository extends JpaRepository<Report, Long> {
    List<Report> findByPatientId(Long patientId);
}