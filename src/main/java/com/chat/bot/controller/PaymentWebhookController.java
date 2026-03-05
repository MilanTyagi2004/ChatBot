package com.chat.bot.controller;

import com.chat.bot.dto.BookingContext;
import com.chat.bot.entity.Booking;
import com.chat.bot.entity.BotState;
import com.chat.bot.entity.UserSession;
import com.chat.bot.service.BookingService;
import com.chat.bot.service.SessionService;
import com.chat.bot.service.WhatsappService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/payment")
public class PaymentWebhookController {

    private final SessionService sessionService;
    private final BookingService bookingService;
    private final WhatsappService whatsAppService;
    private final ObjectMapper objectMapper;

    @Value("${razorpay.webhook-secret}")
    private String webhookSecret;

    @PostMapping("/webhook")
    public ResponseEntity<String> handlePayment(
            @RequestBody String rawBody,
            @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature) {

        log.info("Payment webhook received");

        // ==============================
        // Verify Razorpay signature
        // ==============================

        if (signature == null || !verifySignature(rawBody, signature)) {
            log.warn("Invalid or missing Razorpay webhook signature");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("invalid signature");
        }

        // ==============================
        // Parse body
        // ==============================

        Map<String, Object> body;
        try {
            body = objectMapper.readValue(rawBody, Map.class);
        } catch (Exception e) {
            log.error("Failed to parse webhook body", e);
            return ResponseEntity.badRequest().body("invalid body");
        }

        // ==============================
        // Validate event type
        // ==============================

        String event = (String) body.get("event");

        if (event == null) {
            log.warn("Webhook event is null, ignoring");
            return ResponseEntity.ok("ignored");
        }

        if (!event.equals("payment_link.paid")) {
            log.info("Ignoring non-payment event: {}", event);
            return ResponseEntity.ok("ignored");
        }

        // ==============================
        // Extract payload safely
        // ==============================

        Map payload = (Map) body.get("payload");
        if (payload == null)
            return ResponseEntity.ok("no payload");

        Map paymentLink = (Map) payload.get("payment_link");
        if (paymentLink == null)
            return ResponseEntity.ok("no payment_link");

        Map entity = (Map) paymentLink.get("entity");
        if (entity == null)
            return ResponseEntity.ok("no entity");

        Map customer = (Map) entity.get("customer");
        if (customer == null)
            return ResponseEntity.ok("no customer");

        String phone = customer.get("contact").toString();

        // Extract Razorpay payment ID from payload.payment.entity.id
        Map payment = (Map) payload.get("payment");
        String razorpayPaymentId = null;
        if (payment != null) {
            Map paymentEntity = (Map) payment.get("entity");
            if (paymentEntity != null) {
                razorpayPaymentId = paymentEntity.get("id").toString();
            }
        }

        log.info("Payment confirmed for phone={}, razorpayPaymentId={}", phone, razorpayPaymentId);

        // ==============================
        // Retrieve session
        // ==============================

        UserSession session = sessionService.getOrCreate(phone);

        BookingContext context = sessionService.getContext(session);

        // ==============================
        // Confirm the PENDING booking
        // ==============================

        Booking confirmedBooking = bookingService.confirmBooking(phone, razorpayPaymentId);

        if (confirmedBooking != null) {

            sessionService.updateState(session, BotState.BOOKING_CONFIRMED);

            // ==============================
            // Send confirmation
            // ==============================

            String message = """
                    ✅ Payment received!

                    Your appointment is confirmed.

                    📋 Service: %s
                    📍 Location: %s
                    📅 Date: %s
                    🕐 Time: %s

                    Thank you for choosing Rohan Dental Care!
                    For any changes, please contact us at +91 8792128350 , rohandentalcare@gmail.com
                    """.formatted(
                    confirmedBooking.getService(),
                    confirmedBooking.getLocation(),
                    confirmedBooking.getAppointmentDate(),
                    confirmedBooking.getAppointmentTime());

            whatsAppService.sendMessage(phone, message);

            log.info("Booking confirmed and message sent for phone={}", phone);

            return ResponseEntity.ok("success");

        } else {

            // No PENDING booking found — context might still have data
            log.warn("No PENDING booking found for phone={}, context may be stale", phone);

            whatsAppService.sendMessage(
                    phone,
                    "Payment received but no pending reservation found. " +
                            "The clinic will contact you to arrange your appointment.");

            return ResponseEntity.ok("no pending booking");
        }
    }

    // ==============================
    // Razorpay Signature Verification
    // ==============================

    private boolean verifySignature(String payload, String expectedSignature) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(webhookSecret.getBytes(), "HmacSHA256");
            mac.init(secretKey);
            byte[] hash = mac.doFinal(payload.getBytes());

            String generatedSignature = bytesToHex(hash);
            return generatedSignature.equals(expectedSignature);
        } catch (Exception e) {
            log.error("Signature verification failed", e);
            return false;
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}