package com.chat.bot.service;

import com.chat.bot.dto.BookingStatsDTO;
import com.chat.bot.entity.Booking;
import com.chat.bot.entity.BookingStatus;
import com.chat.bot.entity.PaymentStatus;
import com.chat.bot.exception.SlotAlreadyBookedException;
import com.chat.bot.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;

    // ================================
    // SERVICE MAPPINGS
    // ================================

    public String mapService(String option) {
        return switch (option) {
            case "1" -> "Consultation";
            case "2" -> "Cleaning";
            case "3" -> "Root Canal";
            case "4" -> "Implants";
            default -> null;
        };
    }

    public String mapLocation(String option) {
        return switch (option) {
            case "1" -> "Vijayawada";
            case "2" -> "Jaggayyapeta";
            default -> null;
        };
    }

    public String mapTime(String option) {
        return switch (option) {
            case "1" -> "10:00 AM";
            case "2" -> "4:00 PM";
            default -> null;
        };
    }

    public LocalDate mapDate(String option) {
        return switch (option) {
            case "1" -> LocalDate.now().plusDays(1);
            case "2" -> LocalDate.now().plusDays(2);
            default -> null;
        };
    }

    // ================================
    // RESERVE SLOT (PENDING booking)
    // ================================

    @Transactional
    public Booking reserveSlot(String phone,
            String name,
            String service,
            String location,
            LocalDate date,
            String time) {

        // Clean up expired PENDING bookings (older than 15 minutes)
        cleanupExpiredReservations();

        Booking booking = new Booking();
        booking.setPhoneNumber(phone);
        booking.setPatientName(name);
        booking.setService(service);
        booking.setLocation(location);
        booking.setAppointmentDate(date);
        booking.setAppointmentTime(time);
        booking.setPaymentStatus(PaymentStatus.PENDING);
        booking.setBookingStatus(BookingStatus.PENDING);
        booking.setCreatedAt(LocalDateTime.now());

        try {
            return bookingRepository.save(booking);
        } catch (DataIntegrityViolationException e) {
            throw new SlotAlreadyBookedException("Selected slot is already reserved or booked.");
        }
    }

    // ================================
    // CONFIRM BOOKING (after payment)
    // ================================

    @Transactional
    public Booking confirmBooking(String phone, String razorpayPaymentId) {

        Booking booking = bookingRepository
                .findFirstByPhoneNumberAndBookingStatus(phone, BookingStatus.PENDING)
                .orElse(null);

        if (booking == null) {
            log.warn("No PENDING booking found for phone={}", phone);
            return null;
        }

        booking.setBookingStatus(BookingStatus.CONFIRMED);
        booking.setPaymentStatus(PaymentStatus.PAID);
        booking.setRazorpayPaymentId(razorpayPaymentId);

        return bookingRepository.save(booking);
    }

    // ================================
    // CREATE BOOKING (direct, no reservation)
    // ================================

    @Transactional
    public Booking createBooking(String phone,
            String name,
            String service,
            String location,
            LocalDate date,
            String time,
            String razorpayPaymentId,
            PaymentStatus paymentStatus) {

        Booking booking = new Booking();

        booking.setPhoneNumber(phone);
        booking.setPatientName(name);
        booking.setService(service);
        booking.setLocation(location);
        booking.setAppointmentDate(date);
        booking.setAppointmentTime(time);
        booking.setPaymentStatus(paymentStatus);
        booking.setRazorpayPaymentId(razorpayPaymentId);
        booking.setBookingStatus(BookingStatus.CONFIRMED);
        booking.setCreatedAt(LocalDateTime.now());

        try {
            return bookingRepository.save(booking);
        } catch (DataIntegrityViolationException e) {
            throw new SlotAlreadyBookedException("Selected slot is already booked.");
        }
    }

    // ================================
    // DUPLICATE & SLOT CHECKS
    // ================================

    public boolean hasActiveBooking(String phone) {
        return !bookingRepository
                .findByPhoneNumberAndBookingStatus(phone, BookingStatus.CONFIRMED)
                .isEmpty();
    }

    public boolean isSlotOccupied(String location, LocalDate date, String time) {
        // Clean up expired reservations before checking
        cleanupExpiredReservations();

        return bookingRepository
                .existsByLocationAndAppointmentDateAndAppointmentTimeAndBookingStatusIn(
                        location, date, time,
                        List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED, BookingStatus.COMPLETED));
    }

    // ================================
    // CLEANUP EXPIRED RESERVATIONS
    // ================================

    @Transactional
    public void cleanupExpiredReservations() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(15);
        bookingRepository.deleteByBookingStatusAndCreatedAtBefore(BookingStatus.PENDING, cutoff);
    }

    // ================================
    // ADMIN FILTERS
    // ================================

    public List<Booking> getBookingsByDate(LocalDate date) {
        return bookingRepository.findByAppointmentDate(date);
    }

    public List<Booking> getBookingsByLocation(String location) {
        return bookingRepository.findByLocation(location);
    }

    public List<Booking> getAllBookings() {
        return bookingRepository.findAll();
    }

    public void cancelBooking(String id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        booking.setBookingStatus(BookingStatus.CANCELLED);
        // Free up the slot by modifying the time slightly to avoid DB UniqueConstraint
        booking.setAppointmentTime(booking.getAppointmentTime() + " (Cancelled)");
        bookingRepository.save(booking);
    }

    // ================================
    // ADMIN: BOOKING STATS
    // ================================

    private static final List<BookingStatus> REAL_BOOKING_STATUSES =
            List.of(BookingStatus.CONFIRMED, BookingStatus.COMPLETED);

    public BookingStatsDTO getBookingStats() {
        LocalDate today = LocalDate.now();
        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDate yearStart = today.withDayOfYear(1);

        long todayCount = bookingRepository
                .countByBookingStatusInAndAppointmentDate(REAL_BOOKING_STATUSES, today);

        long monthCount = bookingRepository
                .countByBookingStatusInAndAppointmentDateBetween(REAL_BOOKING_STATUSES, monthStart, today);

        long yearCount = bookingRepository
                .countByBookingStatusInAndAppointmentDateBetween(REAL_BOOKING_STATUSES, yearStart, today);

        return new BookingStatsDTO(todayCount, monthCount, yearCount);
    }

    // ================================
    // ADMIN: COMPLETE BOOKING
    // ================================

    @Transactional
    public Booking completeBooking(String bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        if (booking.getBookingStatus() != BookingStatus.CONFIRMED) {
            throw new RuntimeException("Only confirmed bookings can be marked as completed");
        }

        booking.setBookingStatus(BookingStatus.COMPLETED);
        return bookingRepository.save(booking);
    }

    // ================================
    // ADMIN: LIST BOOKINGS BY STATUS
    // ================================

    public List<Booking> getConfirmedBookings() {
        return bookingRepository.findByBookingStatusOrderByAppointmentDateAsc(BookingStatus.CONFIRMED);
    }

    public List<Booking> getCompletedBookings() {
        return bookingRepository.findByBookingStatusOrderByAppointmentDateDesc(BookingStatus.COMPLETED);
    }
}