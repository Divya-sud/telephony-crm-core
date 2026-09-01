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
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class AriTelephonyService {

    private final CallLogRepository callLogRepository;
    private final ObjectMapper mapper = new ObjectMapper();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    @Value("${telephony.asterisk.base-url}")
    private String ariBaseUrl;

    @Value("${telephony.asterisk.username}")
    private String username;

    @Value("${telephony.asterisk.password}")
    private String password;

    @Value("${telephony.asterisk.app-name}")
    private String appName;

    private RestClient restClient;
    private volatile boolean isConnected = false;

    @PostConstruct
    public void init() {
        String auth = Base64.getEncoder().encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));
        this.restClient = RestClient.builder()
                .baseUrl(ariBaseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Basic " + auth)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("ngrok-skip-browser-warning", "true")
                .defaultHeader("User-Agent", "Spring-ARI-Client")
                .build();

        // Start connection loop with auto-reconnect
        scheduler.scheduleWithFixedDelay(this::ensureWebSocketConnected, 2, 10, TimeUnit.SECONDS);
    }

    private synchronized void ensureWebSocketConnected() {
        if (isConnected) {
            return;
        }

        try {
            // Build Asterisk ARI WebSocket URL with all required query parameters
            String cleanWsUrl = ariBaseUrl
                    .replace("https://", "wss://")
                    .replace("http://", "ws://");

            String wsEndpoint = String.format("%s/events?app=%s&api_key=%s:%s&subscribeAll=true",
                    cleanWsUrl, appName, username, password);

            log.info("Connecting to Asterisk ARI WebSocket: {} (App: {})", cleanWsUrl, appName);

            WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
            headers.add("ngrok-skip-browser-warning", "true");
            headers.add("User-Agent", "Spring-ARI-Client");

            StandardWebSocketClient client = new StandardWebSocketClient();
            client.execute(new TextWebSocketHandler() {
                @Override
                public void afterConnectionEstablished(WebSocketSession session) {
                    isConnected = true;
                    log.info(">>> ACTIVE: Successfully connected to Asterisk ARI Stasis App [{}] <<<", appName);
                }

                @Override
                protected void handleTextMessage(WebSocketSession session, TextMessage message) {
                    processEvent(message.getPayload());
                }

                @Override
                public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
                    isConnected = false;
                    log.warn("Asterisk ARI WebSocket closed (Status: {}). Reconnecting in next cycle...", status);
                }

                @Override
                public void handleTransportError(WebSocketSession session, Throwable exception) {
                    isConnected = false;
                    log.error("ARI WebSocket transport error: {}", exception.getMessage());
                }
            }, headers, URI.create(wsEndpoint)).get(5, TimeUnit.SECONDS);

        } catch (Exception e) {
            isConnected = false;
            log.warn("Asterisk ARI WebSocket connection attempt failed: {}. Retrying...", e.getMessage());
        }
    }

    private void processEvent(String payload) {
        try {
            JsonNode node = mapper.readTree(payload);
            String type = node.path("type").asText();

            log.info("Received Asterisk ARI Event: {}", type);

            if ("StasisStart".equals(type)) {
                String channelId = node.path("channel").path("id").asText();
                String callerNumber = node.path("channel").path("caller").path("number").asText();

                log.info("Call entered Stasis app! Channel ID: {}, Caller: {}", channelId, callerNumber);

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

    public void bridgeToAgent(String customerNumber, String agentExt) {
        try {
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
            log.warn("Failed to originate live ARI channel: {}", e.getMessage());
        }
    }
}