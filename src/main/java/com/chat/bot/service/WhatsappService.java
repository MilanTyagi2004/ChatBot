package com.chat.bot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class WhatsappService {

        @Value("${whatsapp.access-token}")
        private String accessToken;

        @Value("${whatsapp.phone-number-id}")
        private String phoneNumberId;

        private final RestTemplate restTemplate;

        // ================================
        // PLAIN TEXT MESSAGE
        // ================================

        public void sendMessage(String to, String message) {

                String url = getApiUrl();

                Map<String, Object> body = Map.of(
                                "messaging_product", "whatsapp",
                                "to", to,
                                "type", "text",
                                "text", Map.of("body", message));

                post(url, body);
        }

        // ================================
        // INTERACTIVE BUTTONS (max 3)
        // ================================

        /**
         * Sends an interactive button message.
         * Each button is a Map with "id" and "title" keys.
         */
        public void sendInteractiveButtons(String to,
                        String bodyText,
                        List<Map<String, String>> buttons) {

                List<Map<String, Object>> buttonPayload = new ArrayList<>();
                for (Map<String, String> btn : buttons) {
                        buttonPayload.add(Map.of(
                                        "type", "reply",
                                        "reply", Map.of(
                                                        "id", btn.get("id"),
                                                        "title", btn.get("title"))));
                }

                Map<String, Object> body = new LinkedHashMap<>();
                body.put("messaging_product", "whatsapp");
                body.put("to", to);
                body.put("type", "interactive");
                body.put("interactive", Map.of(
                                "type", "button",
                                "body", Map.of("text", bodyText),
                                "action", Map.of("buttons", buttonPayload)));

                post(getApiUrl(), body);
        }

        // ================================
        // INTERACTIVE LIST (4+ options)
        // ================================

        /**
         * Sends an interactive list message.
         * Each row is a Map with "id" and "title" keys.
         */
        public void sendInteractiveList(String to,
                        String bodyText,
                        String buttonLabel,
                        String sectionTitle,
                        List<Map<String, String>> rows) {

                List<Map<String, Object>> rowPayload = new ArrayList<>();
                for (Map<String, String> row : rows) {
                        rowPayload.add(Map.of(
                                        "id", row.get("id"),
                                        "title", row.get("title")));
                }

                Map<String, Object> section = Map.of(
                                "title", sectionTitle,
                                "rows", rowPayload);

                Map<String, Object> body = new LinkedHashMap<>();
                body.put("messaging_product", "whatsapp");
                body.put("to", to);
                body.put("type", "interactive");
                body.put("interactive", Map.of(
                                "type", "list",
                                "body", Map.of("text", bodyText),
                                "action", Map.of(
                                                "button", buttonLabel,
                                                "sections", List.of(section))));

                post(getApiUrl(), body);
        }

        // ================================
        // INTERNALS
        // ================================

        private String getApiUrl() {
                return "https://graph.facebook.com/v18.0/" + phoneNumberId + "/messages";
        }

        private void post(String url, Map<String, Object> body) {
                try {
                        HttpHeaders headers = new HttpHeaders();
                        headers.setBearerAuth(accessToken);
                        headers.setContentType(MediaType.APPLICATION_JSON);

                        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
                        ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

                        log.info("WhatsApp API response: status={}", response.getStatusCode());
                } catch (Exception e) {
                        log.error("Failed to send WhatsApp message to {}: {}",
                                        body.get("to"), e.getMessage(), e);
                }
        }
}