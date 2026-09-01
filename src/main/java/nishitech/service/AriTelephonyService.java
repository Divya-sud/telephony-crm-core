package nishitech.service;

import nishitech.entity.CallLog;
import nishitech.repository.CallLogRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class AriTelephonyService {

    private final CallLogRepository callLogRepository;
    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${telephony.asterisk.base-url}")
    private String ariBaseUrl;

    @Value("${telephony.asterisk.username}")
    private String username;

    @Value("${telephony.asterisk.password}")
    private String password;

    @Value("${telephony.asterisk.app-name}")
    private String appName;

    private RestClient restClient;

    @PostConstruct
    public void init() {
        String auth = Base64.getEncoder().encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));
        this.restClient = RestClient.builder()
                .baseUrl(ariBaseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Basic " + auth)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();

        connectWebSocket();
    }

    private void connectWebSocket() {
        String wsUrl = ariBaseUrl.replace("http", "ws") + "/events?api_key=" + username + ":" + password + "&app=" + appName;
        StandardWebSocketClient client = new StandardWebSocketClient();
        try {
            client.execute(new TextWebSocketHandler() {
                @Override
                protected void handleTextMessage(WebSocketSession session, TextMessage message) {
                    processEvent(message.getPayload());
                }
            }, wsUrl);
            log.info("Successfully connected to Asterisk ARI WebSocket");
        } catch (Exception e) {
            log.warn("Asterisk ARI unreachable. Waiting for service availability.");
        }
    }

    private void processEvent(String payload) {
        try {
            JsonNode node = mapper.readTree(payload);
            String type = node.path("type").asText();

            if ("StasisStart".equals(type)) {
                String channelId = node.path("channel").path("id").asText();
                String callerNumber = node.path("channel").path("caller").path("number").asText();

                // 1. Answer channel
                restClient.post().uri("/channels/{id}/answer", channelId).retrieve().toBodilessEntity();

                // 2. Start recording
                String recordingName = "call_" + channelId;
                restClient.post()
                        .uri("/channels/{id}/record?name={name}&format=wav&ifExists=overwrite", channelId, recordingName)
                        .retrieve().toBodilessEntity();

                // 3. Play greeting
                restClient.post()
                        .uri("/channels/{id}/play?media=sound:demo-congrats", channelId)
                        .retrieve().toBodilessEntity();

                // 4. Persist call log
                callLogRepository.save(CallLog.builder()
                        .channelId(channelId)
                        .callerNumber(callerNumber)
                        .recordingFileName(recordingName + ".wav")
                        .build());
            }
        } catch (Exception e) {
            log.error("Error processing ARI Event", e);
        }
    }

    /**
     * Bridges an outbound call to an agent extension via Asterisk ARI.
     */
    public void bridgeToAgent(String customerNumber, String agentExt) {
        try {
            // Originate channel to Agent Endpoint and route to Stasis application
            restClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/channels")
                            .queryParam("endpoint", "PJSIP/" + agentExt)
                            .queryParam("extension", customerNumber)
                            .queryParam("context", "default")
                            .queryParam("priority", "1")
                            .queryParam("app", appName)
                            .build())
                    .retrieve()
                    .toBodilessEntity();

            log.info("Originated call bridge to agent {} for destination {}", agentExt, customerNumber);
        } catch (Exception e) {
            log.warn("Failed to originate live ARI channel (Asterisk offline or endpoint not registered): {}", e.getMessage());
        }
    }
}