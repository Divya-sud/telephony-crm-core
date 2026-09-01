package nishitech.controller;

import nishitech.entity.CallLog;
import nishitech.service.AriTelephonyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/telephony")
@RequiredArgsConstructor
public class TelephonyController {

    private final AriTelephonyService telephonyService;

    @PostMapping("/originate")
    public ResponseEntity<?> originateCall(@RequestParam String customerNumber,
                                           @RequestParam(defaultValue = "1001") String agentExt) {
        telephonyService.bridgeToAgent(customerNumber, agentExt);
        return ResponseEntity.ok(Map.of(
                "status", "ORIGINATED",
                "customerNumber", customerNumber,
                "agentExtension", agentExt
        ));
    }

    @GetMapping("/calls")
    public ResponseEntity<List<CallLog>> getCallLogs() {
        return ResponseEntity.ok(telephonyService.getAllCalls());
    }
}