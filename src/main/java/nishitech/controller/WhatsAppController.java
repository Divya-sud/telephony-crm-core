package nishitech.controller;

import nishitech.service.WhatsAppService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/whatsapp")
@RequiredArgsConstructor
public class WhatsAppController {

    private final WhatsAppService whatsAppService;

    @PostMapping("/bulk")
    public ResponseEntity<String> sendBulkMessages(
            @RequestBody List<String> phoneNumbers,
            @RequestParam String templateName,
            @RequestParam(defaultValue = "en_US") String lang) {

        whatsAppService.sendBulk(phoneNumbers, templateName, lang);
        return ResponseEntity.ok("Bulk dispatch queued for " + phoneNumbers.size() + " recipients.");
    }
}