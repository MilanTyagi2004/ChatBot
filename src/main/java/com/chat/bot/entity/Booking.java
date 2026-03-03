package com.chat.bot.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "bookings",
        uniqueConstraints = @UniqueConstraint(columnNames = {"location","appointment_date","appointment_time"}))
@Getter
@Setter
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String phoneNumber;
    private String patientName;
    private String service;
    private String location;

    private LocalDate appointmentDate;
    private String appointmentTime;

    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus;

    @Enumerated(EnumType.STRING)
    private BookingStatus bookingStatus;

    private String razorpayPaymentId;

    private LocalDateTime createdAt;
}