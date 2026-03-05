package com.chat.bot.repository;

import com.chat.bot.entity.Booking;
import com.chat.bot.entity.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, String> {
    boolean existsByLocationAndAppointmentDateAndAppointmentTime(
            String location,
            LocalDate date,
            String time);

    boolean existsByLocationAndAppointmentDateAndAppointmentTimeAndBookingStatusIn(
            String location,
            LocalDate date,
            String time,
            List<BookingStatus> statuses);

    List<Booking> findByPhoneNumberAndBookingStatus(String phoneNumber, BookingStatus status);

    Optional<Booking> findFirstByPhoneNumberAndBookingStatus(String phoneNumber, BookingStatus status);

    List<Booking> findByAppointmentDate(LocalDate date);

    List<Booking> findByLocation(String location);

    void deleteByBookingStatusAndCreatedAtBefore(BookingStatus status, LocalDateTime cutoff);
}
