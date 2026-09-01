package nishitech.service;

import nishitech.dto.LeadAnalysisResult;
import nishitech.dto.TelecallerSuggestion;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    @Value("${grok.api-key:YOUR_GROK_API_KEY}")
    private String grokApiKey;

    private final ObjectMapper mapper = new ObjectMapper();

    public LeadAnalysisResult analyzeLead(String source, String rawData) {
        if ("YOUR_GROK_API_KEY".equals(grokApiKey)) {
            return new LeadAnalysisResult(
                    85,
                    "HOT",
                    List.of("What is your implementation timeline?", "What is your allocated budget?"),
                    "High intent prospect (Grok key not set, default profile applied)."
            );
        }

        try {
            RestClient client = RestClient.builder()
                    .baseUrl("https://api.x.ai/v1")
                    .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + grokApiKey)
                    .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .build();

            Map<String, Object> body = Map.of(
                    "model", "grok-beta",
                    "messages", List.of(
                            Map.of("role", "system", "content", "You are an expert sales analyst. Return ONLY raw JSON with keys: leadScore (number 1-100), intentCategory (HOT/WARM/COLD), suggestedQuestions (array of strings), qualificationSummary (string). Do not include markdown codeblocks."),
                            Map.of("role", "user", "content", "Source: " + (source != null ? source : "UNKNOWN") + " | Lead Data: " + (rawData != null ? rawData : "None"))
                    )
            );

            String rawResponse = client.post()
                    .uri("/chat/completions")
                    .body(body)
                    .retrieve()
                    .body(String.class);

            JsonNode rootNode = mapper.readTree(rawResponse);
            String content = rootNode.path("choices").get(0).path("message").path("content").asText();

            // Clean markdown wrapping if present
            content = content.replace("```json", "").replace("```", "").trim();

            return mapper.readValue(content, LeadAnalysisResult.class);
        } catch (Exception e) {
            log.error("Grok lead analysis failed: {}", e.getMessage());
            return new LeadAnalysisResult(
                    70,
                    "WARM",
                    List.of("What is your expected start date?", "Who is the primary decision maker?"),
                    "Standard analysis assigned (Fallback)."
            );
        }
    }

    public TelecallerSuggestion generateCallerSuggestions(String leadContext, String customerStatement) {
        if ("YOUR_GROK_API_KEY".equals(grokApiKey)) {
            return new TelecallerSuggestion(
                    "Acknowledge objection and state unique differentiation.",
                    "Highlight dedicated SLA support and reliable cloud infrastructure.",
                    "Would you like to schedule a 15-minute product demonstration today?"
            );
        }

        try {
            RestClient client = RestClient.builder()
                    .baseUrl("https://api.x.ai/v1")
                    .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + grokApiKey)
                    .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .build();

            Map<String, Object> body = Map.of(
                    "model", "grok-beta",
                    "messages", List.of(
                            Map.of("role", "system", "content", "You are an active call copilot for telecallers. Return ONLY raw JSON with keys: recommendedAction (string), objectionsHandling (string), closingScript (string). Do not include markdown codeblocks."),
                            Map.of("role", "user", "content", "Lead Context: " + leadContext + " | Customer Statement: " + customerStatement)
                    )
            );

            String rawResponse = client.post()
                    .uri("/chat/completions")
                    .body(body)
                    .retrieve()
                    .body(String.class);

            JsonNode rootNode = mapper.readTree(rawResponse);
            String content = rootNode.path("choices").get(0).path("message").path("content").asText();

            // Clean markdown wrapping if present
            content = content.replace("```json", "").replace("```", "").trim();

            return mapper.readValue(content, TelecallerSuggestion.class);
        } catch (Exception e) {
            log.error("Grok copilot suggestion failed: {}", e.getMessage());
            return new TelecallerSuggestion(
                    "Address the concern directly and offer immediate reassurance.",
                    "Emphasize guaranteed uptime and specialized team onboarding.",
                    "Can we start with a pilot test this week to confirm it fits your requirements?"
            );
        }
    }
}