package nishitech.controller;

import nishitech.crm.CrmAdapter;
import nishitech.dto.LeadAnalysisResult;
import nishitech.dto.TelecallerSuggestion;
import nishitech.entity.Lead;
import nishitech.service.OllamaAiService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class LeadWebhookController {

    private final OllamaAiService aiService;
    private final List<CrmAdapter> crmAdapters;

    @PostMapping("/leads/webhook/{source}")
    public ResponseEntity<Lead> receiveLead(@PathVariable String source, @RequestBody Map<String, Object> payload) {
        LeadAnalysisResult analysis = aiService.analyzeLead(source, payload.toString());

        Lead lead = Lead.builder()
                .source(source.toUpperCase())
                .fullName(String.valueOf(payload.getOrDefault("full_name", "Unknown")))
                .phoneNumber(String.valueOf(payload.getOrDefault("phone_number", "")))
                .email(String.valueOf(payload.getOrDefault("email", "")))
                .rawLeadData(payload.toString())
                .aiLeadScore(analysis.leadScore())
                .intentCategory(analysis.intentCategory())
                .aiAgentAdvice(String.join(" | ", analysis.suggestedQuestions()))
                .build();

        crmAdapters.forEach(adapter -> adapter.syncLead(lead));
        return ResponseEntity.ok(lead);
    }

    @PostMapping("/telecaller/copilot")
    public ResponseEntity<TelecallerSuggestion> getAdvice(
            @RequestParam String leadContext,
            @RequestParam String customerStatement) {
        return ResponseEntity.ok(aiService.generateCallerSuggestions(leadContext, customerStatement));
    }
}