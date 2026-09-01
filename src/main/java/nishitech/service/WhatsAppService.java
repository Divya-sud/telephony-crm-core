package nishitech.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class WhatsAppService {

    @Value("${whatsapp.api-url}")
    private String apiUrl;

    @Value("${whatsapp.phone-number-id}")
    private String phoneNumberId;

    @Value("${whatsapp.access-token}")
    private String accessToken;

    public void sendTemplateMessage(String recipientPhone, String templateName, String lang) {
        RestClient client = RestClient.builder()
                .baseUrl(apiUrl + "/" + phoneNumberId)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();

        Map<String, Object> body = Map.of(
                "messaging_product", "whatsapp",
                "to", recipientPhone,
                "type", "template",
                "template", Map.of(
                        "name", templateName,
                        "language", Map.of("code", lang)
                )
        );

        try {
            client.post().uri("/messages").body(body).retrieve().toBodilessEntity();
            log.info("Dispatched WhatsApp template to {}", recipientPhone);
        } catch (Exception e) {
            log.error("Failed to send WhatsApp message: {}", e.getMessage());
        }
    }

    @Async
    public void sendBulk(List<String> phoneNumbers, String templateName, String lang) {
        for (String phone : phoneNumbers) {
            sendTemplateMessage(phone, templateName, lang);
            try {
                Thread.sleep(50);
            } catch (InterruptedException ignored) {}
        }
    }
}