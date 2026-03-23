# 🦷 Dental Appointment Booking Bot

> A fully automated WhatsApp-based appointment booking system for dental clinics — with real-time payment processing and an admin dashboard.

[![Java](https://img.shields.io/badge/Java-17-orange?style=flat-square&logo=java)](https://www.java.com)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen?style=flat-square&logo=springboot)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-blue?style=flat-square&logo=postgresql)](https://www.postgresql.org)
[![Railway](https://img.shields.io/badge/Deployed%20on-Railway-blueviolet?style=flat-square)](https://railway.app)

---

## ✨ Features

- 💬 **WhatsApp-First Booking** — Patients book appointments via Interactive Lists & Buttons. No app required.
- 🔄 **8-State Conversation Flow** — Session-based state machine manages the full booking lifecycle.
- 🔒 **Smart Slot Locking** — Slots are temporarily reserved for 15 minutes. Unpaid reservations auto-expire.
- 💳 **Automated Payments** — Razorpay payment links are generated and bookings auto-confirm on webhook receipt.
- 🖥️ **Admin Dashboard** — React frontend for managing bookings, viewing stats, and tracking analytics.
- 🔐 **Secure APIs** — HMAC-SHA256 webhook verification + Spring Security Basic Auth for admin routes.

---

## 🏗️ Architecture

```
Patient (WhatsApp)
    │
    ▼
WhatsApp Cloud API (Meta Graph v18)
    │ Webhook
    ▼
Spring Boot Backend
├── FlowService        ← Conversation orchestrator
├── SessionService     ← Manages state + JSON context per user
├── BookingService     ← Slot reservation, confirmation, analytics
├── RazorpayService    ← Payment link creation
└── WhatsAppService    ← Message sending (text, lists, buttons)
    │
    ├── PostgreSQL Database
    │     ├── user_sessions  (state + context JSON)
    │     └── bookings       (status, payment, slot)
    │
    └── Razorpay API
           └── Webhook → auto-confirm booking on payment
                          └── Send confirmation to patient via WhatsApp

Admin (Browser)
    │ Basic Auth
    ▼
React Admin Dashboard
    └── REST API → Spring Boot → Booking Stats & Management
```

---

## 📋 Booking Flow

```
1. User sends "Hi" on WhatsApp
2. Bot asks for Name
3. Selects Service       → [Consultation / Cleaning / Root Canal / Implants]
4. Selects Location      → [Vijayawada / Jaggayyapeta]
5. Selects Date          → [Tomorrow / Day After Tomorrow]
6. Selects Time          → [10:00 AM / 4:00 PM]
7. Slot reserved (15 min hold) → Razorpay link sent
8. Patient pays
9. Webhook received → Booking confirmed → Patient notified ✅
```

---

## 🛠️ Tech Stack

| Layer | Technology |
|---|---|
| Backend | Java 17, Spring Boot 3.x, Spring Data JPA, Spring Security |
| Frontend | React, Tailwind CSS |
| Database | MySQL |
| Integrations | WhatsApp Cloud API (Meta), Razorpay |
| Deployment | Railway (Docker) |
| Build Tool | Maven |

---

## 🚀 Getting Started

### Prerequisites
- Java 17+
- Maven
- PostgreSQL
- WhatsApp Business API account (Meta Developer)
- Razorpay account

### Environment Variables

Set the following in `application.properties` or as Railway environment variables:

```properties
# Database
spring.datasource.url=jdbc:postgresql://<host>/<db>
spring.datasource.username=<user>
spring.datasource.password=<password>

# WhatsApp
whatsapp.access-token=<your-meta-access-token>
whatsapp.phone-number-id=<your-phone-number-id>
whatsapp.verify-token=<your-webhook-verify-token>

# Razorpay
razorpay.key-id=<your-razorpay-key>
razorpay.key-secret=<your-razorpay-secret>
razorpay.webhook-secret=<your-webhook-secret>
```

### Run Locally

```bash
git clone https://github.com/<your-username>/ChatBot.git
cd ChatBot
mvn spring-boot:run
```

---

## 📡 API Endpoints

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| `GET` | `/webhook` | Public | WhatsApp webhook verification |
| `POST` | `/webhook` | Public | Receive WhatsApp messages |
| `POST` | `/payment/webhook` | Public | Razorpay payment events |
| `GET` | `/admin/bookings` | Admin | List all bookings |
| `GET` | `/admin/stats` | Admin | Get booking statistics |
| `POST` | `/admin/bookings/{id}/complete` | Admin | Mark booking as completed |
| `POST` | `/admin/bookings/{id}/cancel` | Admin | Cancel a booking |

> API docs available at `/swagger-ui/index.html` after running the app.

---

## 🐳 Docker

```bash
docker build -t dental-bot .
docker run -p 8080:8080 --env-file .env dental-bot
```

---

## 📄 License

MIT License - feel free to use, fork, and build on this project.

