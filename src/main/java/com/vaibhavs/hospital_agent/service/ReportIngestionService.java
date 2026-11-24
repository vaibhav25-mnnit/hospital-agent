package com.vaibhavs.hospital_agent.service;

import com.vaibhavs.hospital_agent.entity.Report;
import com.vaibhavs.hospital_agent.repository.ReportRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@Service
public class ReportIngestionService {

    private static final Logger log = LoggerFactory.getLogger(ReportIngestionService.class);

    private final VectorStore vectorStore;
    private final ReportRepository reports;
    private final Path baseDir;
    private final TokenTextSplitter splitter = new TokenTextSplitter();

    public ReportIngestionService(VectorStore vectorStore, ReportRepository reports,
                                  @Value("${app.storage.dir:storage}") String storageDir) {
        this.vectorStore = vectorStore;
        this.reports = reports;
        this.baseDir = Path.of(storageDir).toAbsolutePath().normalize();
    }

    /** Re-creates the chunks of one report. Returns the number of chunks stored. */
    public int ingest(Report r) {
        remove(r.getId());                                   // never leave stale chunks behind

        Path file = baseDir.resolve(r.getS3Key()).normalize();
        if (!file.startsWith(baseDir)) {                      // s3Key comes from the API: block ../ tricks
            throw new IllegalArgumentException("Invalid report path: " + r.getS3Key());
        }
        if (!Files.isRegularFile(file)) {
            log.warn("Report {} has no file at {}, skipping", r.getId(), file);
            return 0;
        }

        String text;
        try {
            text = Files.readString(file);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        String header = "Report type: " + r.getReportType() + ", date: " + r.getReportDate() + "\n";
        Document source = new Document(header + text, Map.of(
                "kind", "report",
                "patientId", r.getPatientId(),
                "reportId", r.getId(),
                "reportType", r.getReportType(),
                "reportDate", r.getReportDate().toString()));

        List<Document> chunks = splitter.apply(List.of(source));
        vectorStore.add(chunks);
        return chunks.size();
    }

    public void remove(Long reportId) {
        vectorStore.delete(new FilterExpressionBuilder().eq("reportId", reportId).build());
    }

    /** Wipes all report chunks and rebuilds them from the database. */
    public Map<String, Integer> reindexAll() {
        vectorStore.delete(new FilterExpressionBuilder().eq("kind", "report").build());
        int reportCount = 0;
        int chunkCount = 0;
        for (Report r : reports.findAll()) {
            chunkCount += ingest(r);
            reportCount++;
        }
        return Map.of("reports", reportCount, "chunks", chunkCount);
    }
}