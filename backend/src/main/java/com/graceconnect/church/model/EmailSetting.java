package com.graceconnect.church.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "email_settings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "smtp_host")
    @Builder.Default
    private String smtpHost = "smtp.gmail.com";

    @Column(name = "smtp_port")
    @Builder.Default
    private Integer smtpPort = 587;

    @Column(name = "smtp_username")
    private String smtpUsername;

    @Column(name = "smtp_password")
    private String smtpPassword;

    @Column(name = "from_email")
    @Builder.Default
    private String fromEmail = "blessings@graceconnect.org";

    @Column(name = "from_name")
    @Builder.Default
    private String fromName = "GraceConnect Church";

    @Column(name = "pastor_signature")
    @Builder.Default
    private String pastorSignature = "Pastor & GraceConnect Church Family";

    @Column(name = "auto_send_daily", nullable = false)
    @Builder.Default
    private Boolean autoSendDaily = true;

    @Column(name = "last_run_date")
    private LocalDate lastRunDate;
}

