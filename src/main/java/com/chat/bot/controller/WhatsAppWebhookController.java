package com.chat.bot.controller;

import com.chat.bot.service.FlowService;
import com.chat.bot.service.WhatsappService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/webhook")
@RequiredArgsConstructor
public class WhatsAppWebhookController {

    private final FlowService flowService;
    private final WhatsappService whatsappService;

    @Value("${whatsapp.verify-token}")
    private String verifyToken;

    // =========================
    // WEBHOOK VERIFICATION
    // =========================
    @GetMapping
    public ResponseEntity<String> verifyWebhook(
            @RequestParam(value = "hub.mode", required = false) String mode,
            @RequestParam(value = "hub.verify_token", required = false) String token,
            @RequestParam(value = "hub.challenge", required = false) String challenge) {

        log.debug("Webhook verification: mode={}, token={}, challenge={}", mode, token, challenge);

        if ("subscribe".equals(mode) && verifyToken.equals(token)) {
            return ResponseEntity.ok(challenge);
        }

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Verification failed");
    }

    // =========================
    // RECEIVE MESSAGE
    // =========================
    @PostMapping
    public ResponseEntity<Void> receiveMessage(@RequestBody Map<String, Object> payload) {

        try {

            List<?> entryList = (List<?>) payload.get("entry");
            if (entryList == null || entryList.isEmpty())
                return ResponseEntity.ok().build();

            Map<?, ?> entry = (Map<?, ?>) entryList.get(0);
            List<?> changesList = (List<?>) entry.get("changes");
            if (changesList == null || changesList.isEmpty())
                return ResponseEntity.ok().build();

            Map<?, ?> change = (Map<?, ?>) changesList.get(0);
            Map<?, ?> value = (Map<?, ?>) change.get("value");

            List<Map<String, Object>> messages = (List<Map<String, Object>>) value.get("messages");

            if (messages == null || messages.isEmpty()) {
                return ResponseEntity.ok().build();
            }

            Map<String, Object> msg = messages.get(0);

            String phone = (String) msg.get("from");
            String type = (String) msg.get("type");

            String text = null;

            // ================================
            // PARSE TEXT MESSAGES
            // ================================
            if ("text".equals(type)) {
                text = (String) ((Map<?, ?>) msg.get("text")).get("body");
            }

            // ================================
            // PARSE INTERACTIVE BUTTON REPLIES
            // ================================
            else if ("interactive".equals(type)) {
                Map<?, ?> interactive = (Map<?, ?>) msg.get("interactive");
                if (interactive != null) {
                    String interactiveType = (String) interactive.get("type");

                    if ("button_reply".equals(interactiveType)) {
                        Map<?, ?> buttonReply = (Map<?, ?>) interactive.get("button_reply");
                        if (buttonReply != null) {
                            text = (String) buttonReply.get("id");
                        }
                    } else if ("list_reply".equals(interactiveType)) {
                        Map<?, ?> listReply = (Map<?, ?>) interactive.get("list_reply");
                        if (listReply != null) {
                            text = (String) listReply.get("id");
                        }
                    }
                }
            }

            // ================================
            // IGNORE UNSUPPORTED MESSAGE TYPES
            // ================================
            if (text == null) {
                return ResponseEntity.ok().build();
            }

            flowService.handleMessage(phone, text);

        } catch (Exception e) {
            log.error("Error processing webhook message", e);
        }
        return ResponseEntity.ok().build();
    }
}