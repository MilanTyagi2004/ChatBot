package com.chat.bot.service;

import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RazorpayService {

    @Value("${razorpay.key-id}")
    private String keyId;

    @Value("${razorpay.key-secret}")
    private String keySecret;

    @Value("${razorpay.callback-url}")
    private String callbackUrl;

    public String createPaymentLink(String phone) {

        try {

            RazorpayClient razorpay = new RazorpayClient(keyId, keySecret);

            JSONObject request = new JSONObject();
            request.put("amount", 50000); // ₹500 in paise
            request.put("currency", "INR");
            request.put("description", "Dental Consultation Booking");

            JSONObject customer = new JSONObject();
            customer.put("contact", phone);

            request.put("customer", customer);

            request.put("notify", new JSONObject()
                    .put("sms", true)
                    .put("email", false));

            request.put("callback_url", callbackUrl);
            request.put("callback_method", "get");

            var link = razorpay.paymentLink.create(request);

            log.info("Payment link created for phone={}: {}", phone, link.get("short_url"));

            return link.get("short_url").toString();

        } catch (RazorpayException e) {
            log.error("Failed to create payment link for phone={}", phone, e);
            throw new RuntimeException(e);
        }
    }
}