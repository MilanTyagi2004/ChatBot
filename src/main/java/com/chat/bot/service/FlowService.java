package com.chat.bot.service;

import com.chat.bot.dto.BookingContext;
import com.chat.bot.entity.BotState;
import com.chat.bot.entity.UserSession;
import com.chat.bot.exception.SlotAlreadyBookedException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class FlowService {

    private final SessionService sessionService;
    private final BookingService bookingService;
    private final RazorpayService razorpayService;
    private final WhatsappService whatsappService;

    @Transactional
    public void handleMessage(String phone, String message) {

        UserSession session = sessionService.getOrCreate(phone);

        // ================================
        // SESSION TIMEOUT CHECK
        // ================================

        if (sessionService.isTimedOut(session)) {
            sessionService.reset(session);
        }

        // ================================
        // GREETING RESET (restart mid-flow)
        // ================================

        Set<String> greetings = Set.of(
                "hi", "hello", "hey", "hii", "hiii",
                "start", "restart", "menu", "reset");

        if (greetings.contains(message.trim().toLowerCase())
                && session.getState() != BotState.START) {
            sessionService.reset(session);
        }

        sessionService.updateActivity(session);

        // ================================
        // DUPLICATE BOOKING CHECK
        // ================================

        if (session.getState() != BotState.PAYMENT_PENDING
                && session.getState() != BotState.BOOKING_CONFIRMED
                && bookingService.hasActiveBooking(phone)) {

            whatsappService.sendMessage(phone,
                    "You already have a booked appointment. " +
                            "Please contact us if you need to reschedule or cancel.");
            return;
        }

        switch (session.getState()) {

            case START -> start(phone, session);

            case NAME_COLLECTED -> collectName(phone, session, message);

            case SERVICE_SELECTED -> service(phone, session, message);

            case LOCATION_SELECTED -> location(phone, session, message);

            case DATE_SELECTED -> date(phone, session, message);

            case TIME_SELECTED -> time(phone, session, message);

            case PAYMENT_PENDING ->
                whatsappService.sendMessage(phone,
                        "Please complete payment using the link sent.");

            case BOOKING_CONFIRMED ->
                whatsappService.sendMessage(phone,
                        "Your booking is already confirmed. " +
                                "Thank you for choosing Rohan Dental Care!");

            default -> start(phone, session);
        }
    }

    // ================================
    // STEP: WELCOME
    // ================================

    private void start(String phone, UserSession session) {

        sessionService.updateState(session, BotState.NAME_COLLECTED);

        whatsappService.sendMessage(phone,
                "Welcome to Rohan Dental Care 🦷\n\nPlease enter your name to continue.");
    }

    // ================================
    // STEP: COLLECT NAME → SERVICE
    // ================================

    private void collectName(String phone, UserSession session, String message) {

        BookingContext context = sessionService.getContext(session);

        context.setName(message);

        sessionService.saveContext(session, context);

        sessionService.updateState(session, BotState.SERVICE_SELECTED);

        // 4 services → use interactive list
        whatsappService.sendInteractiveList(
                phone,
                "Hi " + message + "! Please select a service:",
                "View Services",
                "Our Services",
                List.of(
                        Map.of("id", "1", "title", "Consultation"),
                        Map.of("id", "2", "title", "Cleaning"),
                        Map.of("id", "3", "title", "Root Canal"),
                        Map.of("id", "4", "title", "Implants")));
    }

    // ================================
    // STEP: SERVICE → LOCATION
    // ================================

    private void service(String phone, UserSession session, String message) {

        String service = bookingService.mapService(message);

        if (service == null) {
            whatsappService.sendMessage(phone, "Invalid option. Please select from the list.");
            return;
        }

        BookingContext context = sessionService.getContext(session);

        context.setService(service);

        sessionService.saveContext(session, context);

        sessionService.updateState(session, BotState.LOCATION_SELECTED);

        // 2 locations → use interactive buttons
        whatsappService.sendInteractiveButtons(
                phone,
                "Select your preferred location:",
                List.of(
                        Map.of("id", "1", "title", "Vijayawada"),
                        Map.of("id", "2", "title", "Jaggayyapeta")));
    }

    // ================================
    // STEP: LOCATION → DATE
    // ================================

    private void location(String phone, UserSession session, String message) {

        String location = bookingService.mapLocation(message);

        if (location == null) {
            whatsappService.sendMessage(phone, "Invalid option. Please select from the buttons.");
            return;
        }

        BookingContext context = sessionService.getContext(session);

        context.setLocation(location);

        sessionService.saveContext(session, context);

        sessionService.updateState(session, BotState.DATE_SELECTED);

        // 2 date options → use interactive buttons
        whatsappService.sendInteractiveButtons(
                phone,
                "Select appointment date:",
                List.of(
                        Map.of("id", "1", "title", "Tomorrow"),
                        Map.of("id", "2", "title", "Day After Tomorrow")));
    }

    // ================================
    // STEP: DATE → TIME
    // ================================

    private void date(String phone, UserSession session, String message) {

        LocalDate date = bookingService.mapDate(message);

        if (date == null) {
            whatsappService.sendMessage(phone, "Invalid option. Please select from the buttons.");
            return;
        }

        BookingContext context = sessionService.getContext(session);

        context.setDate(date.toString());

        sessionService.saveContext(session, context);

        sessionService.updateState(session, BotState.TIME_SELECTED);

        // 2 time options → use interactive buttons
        whatsappService.sendInteractiveButtons(
                phone,
                "Select appointment time:",
                List.of(
                        Map.of("id", "1", "title", "10:00 AM"),
                        Map.of("id", "2", "title", "4:00 PM")));
    }

    // ================================
    // STEP: TIME → PAYMENT (with slot check)
    // ================================

    private void time(String phone, UserSession session, String message) {

        String time = bookingService.mapTime(message);

        if (time == null) {
            whatsappService.sendMessage(phone, "Invalid option. Please select from the buttons.");
            return;
        }

        BookingContext context = sessionService.getContext(session);

        context.setTime(time);

        sessionService.saveContext(session, context);

        // ================================
        // RESERVE SLOT (PENDING booking)
        // ================================

        LocalDate date = LocalDate.parse(context.getDate());

        try {
            bookingService.reserveSlot(
                    phone,
                    context.getName(),
                    context.getService(),
                    context.getLocation(),
                    date,
                    time);
        } catch (SlotAlreadyBookedException e) {

            whatsappService.sendMessage(phone,
                    "Sorry, this slot is already taken. Please choose another time.");

            whatsappService.sendInteractiveButtons(
                    phone,
                    "Select a different time:",
                    List.of(
                            Map.of("id", "1", "title", "10:00 AM"),
                            Map.of("id", "2", "title", "4:00 PM")));
            return;
        }

        // ================================
        // PROCEED TO PAYMENT
        // ================================

        String link = razorpayService.createPaymentLink(session.getPhoneNumber());

        sessionService.updateState(session, BotState.PAYMENT_PENDING);

        String summary = """
                📋 Appointment Summary

                👤 Name: %s
                🦷 Service: %s
                📍 Location: %s
                📅 Date: %s
                🕐 Time: %s

                💰 Booking fee: ₹500

                Complete payment here:
                %s""".formatted(
                context.getName(),
                context.getService(),
                context.getLocation(),
                context.getDate(),
                context.getTime(),
                link);

        whatsappService.sendMessage(phone, summary);
    }
}