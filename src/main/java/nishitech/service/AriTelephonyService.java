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

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.concurrent.CompletionStage;
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
    private WebSocket activeWebSocket;

    @PostConstruct
    public void init() {
        String basicAuth = "Basic " + Base64.getEncoder().encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));

        this.restClient = RestClient.builder()
                .baseUrl(ariBaseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, basicAuth)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("ngrok-skip-browser-warning", "true")
                .build();

        // Heartbeat / auto-reconnect every 5 seconds
        scheduler.scheduleWithFixedDelay(this::ensureWebSocketConnected, 2, 5, TimeUnit.SECONDS);
    }

    private synchronized void ensureWebSocketConnected() {
        if (isConnected && activeWebSocket != null) {
            return;
        }

        try {
            String baseUrlWithAri = ariBaseUrl.endsWith("/ari") ? ariBaseUrl : ariBaseUrl + "/ari";
            String wsBaseUrl = baseUrlWithAri
                    .replace("https://", "wss://")
                    .replace("http://", "ws://");

            String wsEndpoint = String.format("%s/events?app=%s&api_key=%s:%s&subscribeAll=true",
                    wsBaseUrl, appName, username, password);

            String rawAuth = username + ":" + password;
            String encodedAuth = "Basic " + Base64.getEncoder().encodeToString(rawAuth.getBytes(StandardCharsets.UTF_8));

            log.info("Connecting native WebSocket to ARI: {}", wsEndpoint);

            HttpClient httpClient = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();

            httpClient.newWebSocketBuilder()
                    .header("Authorization", encodedAuth)
                    .header("ngrok-skip-browser-warning", "true")
                    .header("User-Agent", "Telephony-CRM-Core")
                    .connectTimeout(Duration.ofSeconds(10))
                    .buildAsync(URI.create(wsEndpoint), new WebSocket.Listener() {
                        private final StringBuilder buffer = new StringBuilder();

                        @Override
                        public void onOpen(WebSocket webSocket) {
                            isConnected = true;
                            activeWebSocket = webSocket;
                            log.info(">>> ACTIVE: Successfully connected to Asterisk ARI Stasis App [{}] <<<", appName);
                            WebSocket.Listener.super.onOpen(webSocket);
                        }

                        @Override
                        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
                            buffer.append(data);
                            if (last) {
                                processEvent(buffer.toString());
                                buffer.setLength(0);
                            }
                            return WebSocket.Listener.super.onText(webSocket, data, last);
                        }

                        @Override
                        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
                            isConnected = false;
                            activeWebSocket = null;
                            log.warn("ARI WebSocket closed: {} - {}", statusCode, reason);
                            return WebSocket.Listener.super.onClose(webSocket, statusCode, reason);
                        }

                        @Override
                        public void onError(WebSocket webSocket, Throwable error) {
                            isConnected = false;
                            activeWebSocket = null;
                            log.error("ARI WebSocket transport error: {}", error.getMessage());
                            WebSocket.Listener.super.onError(webSocket, error);
                        }
                    }).get(10, TimeUnit.SECONDS);

        } catch (Exception e) {
            isConnected = false;
            activeWebSocket = null;
            log.warn("ARI connection attempt failed: {}. Retrying in 5s...", e.getMessage());
        }
    }

    private void processEvent(String payload) {
        try {
            JsonNode node = mapper.readTree(payload);
            String type = node.path("type").asText();

            log.info("Incoming Asterisk ARI Event: {}", type);

            if ("StasisStart".equals(type)) {
                String channelId = node.path("channel").path("id").asText();
                String callerNumber = node.path("channel").path("caller").path("number").asText();

                log.info("Live Call entered Stasis! Channel ID: {}, Caller: {}", channelId, callerNumber);

                // 1. Answer channel
                restClient.post().uri("/channels/{id}/answer", channelId).retrieve().toBodilessEntity();

                // 2. Start recording
                String recordingName = "call_" + channelId;
                restClient.post()
                        .uri("/channels/{id}/record?name={name}&format=wav&ifExists=overwrite", channelId, recordingName)
                        .retrieve().toBodilessEntity();

                // 3. Play greeting sound
                restClient.post()
                        .uri("/channels/{id}/play?media=sound:demo-congrats", channelId)
                        .retrieve().toBodilessEntity();

                // 4. Save call record
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
                            .queryParam("context", "from-internal")
                            .queryParam("priority", "1")
                            .queryParam("app", appName)
                            .build())
                    .retrieve()
                    .toBodilessEntity();

            log.info("Bridged outbound call to agent {} for destination {}", agentExt, customerNumber);
        } catch (Exception e) {
            log.warn("Failed to originate live ARI channel: {}", e.getMessage());
        }
    }
}