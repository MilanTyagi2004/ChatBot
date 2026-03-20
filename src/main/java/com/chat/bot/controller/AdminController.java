package com.chat.bot.controller;

import com.chat.bot.dto.BookingStatsDTO;
import com.chat.bot.entity.Booking;
import com.chat.bot.service.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "Admin dashboard APIs")
public class AdminController {

    private final BookingService bookingService;

    // ================================
    // BOOKING STATS
    // ================================

    @GetMapping("/stats")
    @Operation(summary = "Get booking statistics",
            description = "Returns booking counts for today, this month, and this year")
    public ResponseEntity<BookingStatsDTO> getStats() {
        return ResponseEntity.ok(bookingService.getBookingStats());
    }

    // ================================
    // CONFIRMED (ACTIVE) BOOKINGS
    // ================================

    @GetMapping("/bookings")
    @Operation(summary = "List active bookings",
            description = "Returns all confirmed (active) bookings sorted by appointment date")
    public ResponseEntity<List<Booking>> getConfirmedBookings() {
        return ResponseEntity.ok(bookingService.getConfirmedBookings());
    }

    // ================================
    // COMPLETED BOOKINGS
    // ================================

    @GetMapping("/bookings/completed")
    @Operation(summary = "List completed bookings",
            description = "Returns all completed bookings sorted by most recent first")
    public ResponseEntity<List<Booking>> getCompletedBookings() {
        return ResponseEntity.ok(bookingService.getCompletedBookings());
    }

    // ================================
    // MARK BOOKING AS COMPLETED
    // ================================

    @PutMapping("/bookings/{id}/complete")
    @Operation(summary = "Complete a booking",
            description = "Marks a confirmed booking as completed after the patient's visit")
    public ResponseEntity<Booking> completeBooking(@PathVariable String id) {
        return ResponseEntity.ok(bookingService.completeBooking(id));
    }

    // ================================
    // CANCEL BOOKING
    // ================================

    @PutMapping("/bookings/{id}/cancel")
    @Operation(summary = "Cancel a booking",
            description = "Marks a booking as cancelled and frees up the slot for others to book")
    public ResponseEntity<String> cancelBooking(@PathVariable String id) {
        bookingService.cancelBooking(id);
        return ResponseEntity.ok("Booking cancelled successfully, slot is now free.");
    }
}
