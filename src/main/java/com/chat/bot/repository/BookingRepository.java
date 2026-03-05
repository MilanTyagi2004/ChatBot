package com.chat.bot.repository;

import com.chat.bot.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking,String> {
    boolean existsByLocationAndAppointmentDateAndAppointmentTime(
            String location,
            LocalDate date,
            String time
    );

    List<Booking> findByAppointmentDate(LocalDate date);

    List<Booking> findByLocation(String location);
}
