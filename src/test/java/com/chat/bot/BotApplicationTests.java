package com.chat.bot;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
    "whatsapp.access-token=mock",
    "whatsapp.verify-token=mock",
    "whatsapp.phone-number-id=mock",
    "razorpay.key-id=mock",
    "razorpay.key-secret=mock",
    "razorpay.callback-url=mock",
    "razorpay.webhook-secret=mock"
})
class BotApplicationTests {

	@Test
	void contextLoads() {
	}

}
