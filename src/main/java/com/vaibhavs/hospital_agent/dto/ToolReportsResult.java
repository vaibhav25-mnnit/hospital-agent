package com.vaibhavs.hospital_agent.dto;

import java.util.List;

public record ToolReportsResult(ToolPatient patient, int reportCount, List<ToolReport> reports) {}