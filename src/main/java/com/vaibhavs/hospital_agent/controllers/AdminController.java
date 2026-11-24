package com.vaibhavs.hospital_agent.controllers;

import com.vaibhavs.hospital_agent.service.ReportIngestionService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final ReportIngestionService ingestion;

    public AdminController(ReportIngestionService ingestion) {
        this.ingestion = ingestion;
    }

    @PostMapping("/reindex")
    public Map<String, Integer> reindex() {
        return ingestion.reindexAll();
    }
}