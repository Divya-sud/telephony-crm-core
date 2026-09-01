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
        String basicAuthHeader = "Basic " + Base64.getEncoder().encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));

        this.restClient = RestClient.builder()
                .baseUrl(ariBaseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, basicAuthHeader)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("ngrok-skip-browser-warning", "true")
                .build();

        scheduler.scheduleWithFixedDelay(this::ensureWebSocketConnected, 2, 8, TimeUnit.SECONDS);
    }

    private synchronized void ensureWebSocketConnected() {
        if (isConnected) {
            return;
        }

        try {
            String baseUrlWithAri = ariBaseUrl.endsWith("/ari") ? ariBaseUrl : ariBaseUrl + "/ari";
            String wsBaseUrl = baseUrlWithAri
                    .replace("https://", "wss://")
                    .replace("http://", "ws://");

            // Embed credentials & skip flag in query parameters
            String wsEndpoint = String.format("%s/events?app=%s&api_key=%s:%s&subscribeAll=true&ngrok-skip-browser-warning=true",
                    wsBaseUrl, appName, username, password);

            log.info("Connecting to Asterisk ARI: {}", wsEndpoint);

            WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
            String rawAuth = username + ":" + password;
            headers.add(HttpHeaders.AUTHORIZATION, "Basic " + Base64.getEncoder().encodeToString(rawAuth.getBytes(StandardCharsets.UTF_8)));

            StandardWebSocketClient client = new StandardWebSocketClient();
            client.execute(new TextWebSocketHandler() {
                @Override
                public void afterConnectionEstablished(WebSocketSession session) {
                    isConnected = true;
                    log.info(">>> ACTIVE: Connected to Asterisk ARI Stasis App [{}] <<<", appName);
                }

                @Override
                protected void handleTextMessage(WebSocketSession session, TextMessage message) {
                    processEvent(message.getPayload());
                }

                @Override
                public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
                    isConnected = false;
                    log.warn("Asterisk ARI WebSocket closed ({}). Reconnecting...", status);
                }

                @Override
                public void handleTransportError(WebSocketSession session, Throwable exception) {
                    isConnected = false;
                    log.error("Asterisk ARI transport error: {}", exception.getMessage());
                }
            }, headers, URI.create(wsEndpoint)).get(5, TimeUnit.SECONDS);

        } catch (Exception e) {
            isConnected = false;
            log.warn("Asterisk ARI connection attempt failed: {}", e.getMessage());
        }
    }

    private void processEvent(String payload) {
        try {
            JsonNode node = mapper.readTree(payload);
            String type = node.path("type").asText();

            log.info("ARI Event Received: {}", type);

            if ("StasisStart".equals(type)) {
                String channelId = node.path("channel").path("id").asText();
                String callerNumber = node.path("channel").path("caller").path("number").asText();

                log.info("Call entered Stasis app! Channel: {}, Caller: {}", channelId, callerNumber);

                restClient.post().uri("/channels/{id}/answer", channelId).retrieve().toBodilessEntity();

                String recordingName = "call_" + channelId;
                restClient.post()
                        .uri("/channels/{id}/record?name={name}&format=wav&ifExists=overwrite", channelId, recordingName)
                        .retrieve().toBodilessEntity();

                restClient.post()
                        .uri("/channels/{id}/play?media=sound:demo-congrats", channelId)
                        .retrieve().toBodilessEntity();

                callLogRepository.save(CallLog.builder()
                        .channelId(channelId)
                        .callerNumber(callerNumber)
                        .recordingFileName(recordingName + ".wav")
                        .build());
            }
        } catch (Exception e) {
            log.error("Error handling ARI Event", e);
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

            log.info("Bridged call to agent {} for number {}", agentExt, customerNumber);
        } catch (Exception e) {
            log.warn("Failed to originate live ARI channel: {}", e.getMessage());
        }
    }
}