package com.graceconnect.church.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "whatsapp_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WhatsAppLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "recipient_name", nullable = false)
    private String recipientName;

    @Column(name = "phone_number", nullable = false)
    private String phoneNumber;

    @Column(name = "occasion", nullable = false)
    private String occasion; // BIRTHDAY, ANNIVERSARY, TEST

    @Column(name = "scripture_verse")
    private String scriptureVerse; // e.g. Jeremiah 29:11

    @Column(name = "message_text", columnDefinition = "TEXT")
    private String messageText;

    @Column(name = "status", nullable = false)
    private String status; // SENT, FAILED, SIMULATED

    @Column(name = "provider")
    private String provider; // ULTRAMSG, TWILIO, META_CLOUD, WEBHOOK, SIMULATOR

    @Column(name = "response_payload", columnDefinition = "TEXT")
    private String responsePayload;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "sent_at", nullable = false)
    private LocalDateTime sentAt;
}
