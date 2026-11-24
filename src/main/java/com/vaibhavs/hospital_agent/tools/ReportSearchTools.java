package com.vaibhavs.hospital_agent.tools;

import com.vaibhavs.hospital_agent.dto.ToolExcerpt;
import com.vaibhavs.hospital_agent.dto.ToolSearchResult;
import com.vaibhavs.hospital_agent.service.PatientService;
import org.springframework.ai.document.Document;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Set;

@Component
public class ReportSearchTools {

    private static final Set<String> STOP_WORDS = Set.of(
            "what", "was", "were", "the", "and", "for", "her", "his", "their", "value", "values",
            "level", "levels", "result", "results", "report", "reports", "test", "patient",
            "show", "tell", "about", "find", "does", "have", "with", "from", "that", "this");

    private final VectorStore vectorStore;
    private final PatientService patientService;

    public ReportSearchTools(VectorStore vectorStore, PatientService patientService) {
        this.vectorStore = vectorStore;
        this.patientService = patientService;
    }

    @Tool(description = "Search the text of ONE patient's medical reports for a specific item, "
            + "such as a lab value, finding, impression or recommendation. "
            + "Requires the numeric patient id from findPatients or listPatients. "
            + "Returns up to 4 excerpts with report id, type and date. "
            + "The excerpts are the closest matches and may not contain the answer, "
            + "so check the 'found' flag and that an excerpt mentions the item. "
            + "Do not use this to list reports; use getReports for that.")
    public ToolSearchResult searchReportContent(
            @ToolParam(description = "The numeric patient id") Long patientId,
            @ToolParam(description = "The item to look up in a few words, e.g. 'hemoglobin', "
                    + "'cholesterol' or 'MRI findings'. Do not include the patient's name.") String question) {

        patientService.get(patientId);   // clean "not found" error for an unknown id

        var filter = new FilterExpressionBuilder().eq("patientId", patientId).build();
        List<Document> hits = vectorStore.similaritySearch(SearchRequest.builder()
                .query(question)
                .topK(4)
                .filterExpression(filter)      // enforced in code, not by the model
                .build());

        // keep only excerpts that actually contain a key term from the question
        Set<String> terms = keyTerms(question);
        List<ToolExcerpt> relevant = hits.stream()
                .filter(d -> terms.isEmpty() || containsAny(d.getText(), terms))
                .map(d -> new ToolExcerpt(
                        ((Number) d.getMetadata().get("reportId")).longValue(),
                        String.valueOf(d.getMetadata().get("reportType")),
                        String.valueOf(d.getMetadata().get("reportDate")),
                        d.getText()))
                .toList();

        if (relevant.isEmpty()) {
            return new ToolSearchResult(false,
                    "No report text for this patient mentions \"" + question
                            + "\". Tell the user it was not found. Do not guess or describe other tests.",
                    List.of());
        }
        return new ToolSearchResult(true, "Answer only from these excerpts.", relevant);
    }

    private Set<String> keyTerms(String question) {
        return java.util.Arrays.stream(question.toLowerCase(Locale.ROOT).split("[^a-z0-9]+"))
                .filter(w -> w.length() >= 4 && !STOP_WORDS.contains(w))
                .collect(java.util.stream.Collectors.toSet());
    }

    private boolean containsAny(String text, Set<String> terms) {
        String lower = text.toLowerCase(Locale.ROOT);
        return terms.stream().anyMatch(lower::contains);
    }
}