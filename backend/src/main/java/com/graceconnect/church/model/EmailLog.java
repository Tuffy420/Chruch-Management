package com.graceconnect.church.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "email_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "recipient_name", nullable = false)
    private String recipientName;

    @Column(name = "recipient_email", nullable = false)
    private String recipientEmail;

    @Column(name = "occasion", nullable = false)
    private String occasion; // BIRTHDAY, ANNIVERSARY, TEST

    @Column(name = "subject", nullable = false)
    private String subject;

    @Column(name = "scripture_verse")
    private String scriptureVerse; // e.g. Jeremiah 29:11

    @Column(name = "message_body", columnDefinition = "TEXT")
    private String messageBody;

    @Column(name = "status", nullable = false)
    private String status; // SENT, FAILED, SIMULATED

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "sent_at", nullable = false)
    private LocalDateTime sentAt;
}
