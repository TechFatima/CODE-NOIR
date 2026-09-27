package com.codenoir.demo.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OnboardingReport(
        Project project,
        List<ImportantFolder> importantFolders,
        List<ImportantFile> importantFiles,
        List<Observation> observations,
        List<String> readingOrder,
        String starterTask,
        Map<String, Object> details
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Project(
            String name,
            String summary,
            String primaryLanguage,
            List<String> frameworks,
            String buildTool,
            String architecture
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ImportantFolder(
            String path,
            String purpose
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ImportantFile(
            String path,
            String role,
            String purpose
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Observation(
            String severity,
            String title,
            String message,
            String file
    ) {}
}