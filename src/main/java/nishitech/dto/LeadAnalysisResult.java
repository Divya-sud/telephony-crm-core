package nishitech.dto;

import java.util.List;

public record LeadAnalysisResult(
        int leadScore,
        String intentCategory,
        List<String> suggestedQuestions,
        String qualificationSummary
) {}