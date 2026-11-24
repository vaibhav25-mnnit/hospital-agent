package com.vaibhavs.hospital_agent.dto;

import java.util.List;

public record ToolSearchResult(boolean found, String note, List<ToolExcerpt> excerpts) {}