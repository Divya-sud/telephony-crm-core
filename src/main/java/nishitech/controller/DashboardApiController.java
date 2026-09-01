package nishitech.controller;

import nishitech.dto.LeadAnalysisResult;
import nishitech.dto.TelecallerSuggestion;
import nishitech.entity.*;
import nishitech.repository.*;
import nishitech.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
public class DashboardApiController {

    private final LeadRepository leadRepository;
    private final CallLogRepository callLogRepository;
    private final OllamaAiService aiService;
    private final AriTelephonyService telephonyService;
    private final WhatsAppService whatsAppService;

    // --- LEADS & FILTERING ---
    @GetMapping("/leads")
    public List<Lead> getLeads(@RequestParam(required = false) String type, @RequestParam(required = false) String vertical) {
        List<Lead> all = leadRepository.findAll();
        if (type != null) {
            return all.stream().filter(l -> type.equalsIgnoreCase(l.getIntentCategory())).toList();
        }
        if (vertical != null) {
            return all.stream().filter(l -> vertical.equalsIgnoreCase(l.getVertical())).toList();
        }
        return all;
    }

    @PostMapping("/leads/create")
    public ResponseEntity<Lead> createLead(@RequestBody Lead req) {
        String rawContext = "Vertical: " + req.getVertical() + ", Notes: " + req.getRawLeadData();
        LeadAnalysisResult analysis = aiService.analyzeLead(req.getSource(), rawContext);

        req.setAiLeadScore(analysis.leadScore());
        req.setIntentCategory(analysis.intentCategory());
        req.setAiAgentAdvice(String.join(" | ", analysis.suggestedQuestions()));
        return ResponseEntity.ok(leadRepository.save(req));
    }

    // --- TELEPHONY & CALLING ---
    @GetMapping("/telephony/calls")
    public List<CallLog> getCallLogs() {
        return callLogRepository.findAll();
    }

    @PostMapping("/telephony/originate")
    public ResponseEntity<Map<String, String>> originateCall(@RequestParam String customerNumber, @RequestParam(defaultValue = "1001") String agentExt) {
        telephonyService.bridgeToAgent(customerNumber, agentExt);
        return ResponseEntity.ok(Map.of("status", "ORIGINATED", "customer", customerNumber, "agent", agentExt));
    }

    // --- LIVE AI COPILOT ---
    @PostMapping("/ai/copilot")
    public ResponseEntity<TelecallerSuggestion> copilot(@RequestParam String leadContext, @RequestParam String customerStatement) {
        return ResponseEntity.ok(aiService.generateCallerSuggestions(leadContext, customerStatement));
    }

    // --- MARKETING & WHATSAPP ---
    @PostMapping("/campaigns/whatsapp/bulk")
    public ResponseEntity<Map<String, Object>> bulkWhatsApp(@RequestBody Map<String, String> payload) {
        String[] numbers = payload.getOrDefault("recipients", "").split(",");
        String template = payload.getOrDefault("template", "lead_followup");
        String lang = payload.getOrDefault("lang", "en_US");

        List<String> list = Arrays.stream(numbers).map(String::trim).filter(s -> !s.isEmpty()).toList();
        whatsAppService.sendBulk(list, template, lang);

        return ResponseEntity.ok(Map.of("status", "DISPATCHED", "total", list.size()));
    }

    // --- ANALYTICS SUMMARY ---
    @GetMapping("/analytics/summary")
    public Map<String, Object> getAnalytics() {
        List<Lead> leads = leadRepository.findAll();
        long hot = leads.stream().filter(l -> "HOT".equalsIgnoreCase(l.getIntentCategory())).count();
        long warm = leads.stream().filter(l -> "WARM".equalsIgnoreCase(l.getIntentCategory())).count();
        long cold = leads.stream().filter(l -> "COLD".equalsIgnoreCase(l.getIntentCategory())).count();

        return Map.of(
                "totalLeads", leads.size(),
                "hotLeads", hot,
                "warmLeads", warm,
                "coldLeads", cold,
                "totalCalls", callLogRepository.count(),
                "activeAgents", 1
        );
    }
}