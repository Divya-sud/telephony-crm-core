package nishitech.service;

import nishitech.dto.LeadAnalysisResult;
import nishitech.dto.TelecallerSuggestion;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class OllamaAiService {

    // Reads Render's GROK_API_KEY env variable, or application.yml property, or defaults
    @Value("${GROK_API_KEY:${grok.api-key:YOUR_GROK_API_KEY}}")
    private String grokApiKey;

    private final ObjectMapper mapper = new ObjectMapper();
    private RestClient restClient;

    @PostConstruct
    public void init() {
        this.restClient = RestClient.builder()
                .baseUrl("https://api.x.ai/v1")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public LeadAnalysisResult analyzeLead(String source, String rawData) {
        if (isKeyMissing()) {
            log.warn("GROK_API_KEY not configured on Render. Returning mock lead analysis.");
            return new LeadAnalysisResult(
                    85,
                    "HOT",
                    List.of("When are you looking to start?", "What is your allocated budget?"),
                    "High intent query (Mock analysis - GROK_API_KEY not set)."
            );
        }

        try {
            Map<String, Object> body = Map.of(
                    "model", "grok-beta",
                    "messages", List.of(
                            Map.of("role", "system", "content", "You are an expert sales analyst. Return ONLY a valid JSON object with keys: leadScore (number 1-100), intentCategory (HOT, WARM, or COLD), suggestedQuestions (array of 2-3 strings), and qualificationSummary (string). Do not include markdown code block backticks."),
                            Map.of("role", "user", "content", "Source: " + (source != null ? source : "UNKNOWN") + " | Lead Data: " + (rawData != null ? rawData : "None"))
                    ),
                    "temperature", 0.2
            );

            String rawResponse = restClient.post()
                    .uri("/chat/completions")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + grokApiKey.trim())
                    .body(body)
                    .retrieve()
                    .body(String.class);

            String cleanJson = extractJson(rawResponse);
            return mapper.readValue(cleanJson, LeadAnalysisResult.class);
        } catch (Exception e) {
            log.error("Grok lead analysis failed: {}", e.getMessage());
            return new LeadAnalysisResult(
                    70,
                    "WARM",
                    List.of("What is your timeline for decision?", "Who is the primary contact?"),
                    "Standard analysis fallback due to API error: " + e.getMessage()
            );
        }
    }

    public TelecallerSuggestion generateCallerSuggestions(String leadContext, String customerStatement) {
        if (isKeyMissing()) {
            return new TelecallerSuggestion(
                    "Acknowledge objection and state unique differentiation.",
                    "Highlight dedicated SLA support and reliable cloud infrastructure.",
                    "Would you like to schedule a 15-minute product demonstration today?"
            );
        }

        try {
            Map<String, Object> body = Map.of(
                    "model", "grok-beta",
                    "messages", List.of(
                            Map.of("role", "system", "content", "You are a real-time call copilot for sales agents. Return ONLY a valid JSON object with keys: recommendedAction (string), objectionsHandling (string), and closingScript (string). Do not include markdown code block backticks."),
                            Map.of("role", "user", "content", "Context: " + leadContext + " | Customer said: " + customerStatement)
                    ),
                    "temperature", 0.3
            );

            String rawResponse = restClient.post()
                    .uri("/chat/completions")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + grokApiKey.trim())
                    .body(body)
                    .retrieve()
                    .body(String.class);

            String cleanJson = extractJson(rawResponse);
            return mapper.readValue(cleanJson, TelecallerSuggestion.class);
        } catch (Exception e) {
            log.error("Grok copilot suggestion failed: {}", e.getMessage());
            return new TelecallerSuggestion(
                    "Address the concern directly and offer immediate reassurance.",
                    "Emphasize guaranteed uptime and specialized team onboarding.",
                    "Can we start with a pilot test this week to confirm it fits your requirements?"
            );
        }
    }

    private boolean isKeyMissing() {
        return grokApiKey == null || grokApiKey.isBlank() || "YOUR_GROK_API_KEY".equals(grokApiKey);
    }

    private String extractJson(String rawResponse) throws Exception {
        JsonNode rootNode = mapper.readTree(rawResponse);
        String content = rootNode.path("choices").get(0).path("message").path("content").asText();

        // Strip markdown backticks if returned
        content = content.replaceAll("```json", "").replaceAll("```", "").trim();

        // Ensure we isolate the JSON object if model returns preamble text
        int start = content.indexOf('{');
        int end = content.lastIndexOf('}');
        if (start != -1 && end != -1 && end > start) {
            return content.substring(start, end + 1);
        }
        return content;
    }
}