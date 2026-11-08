package com.graceconnect.church.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "whatsapp_settings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WhatsAppSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "provider")
    @Builder.Default
    private String provider = "ULTRAMSG"; // ULTRAMSG, TWILIO, META_CLOUD, WEBHOOK, SIMULATOR

    @Column(name = "api_url")
    private String apiUrl;

    @Column(name = "instance_id")
    private String instanceId;

    @Column(name = "api_token")
    private String apiToken;

    @Column(name = "account_sid")
    private String accountSid; // For Twilio

    @Column(name = "from_phone")
    private String fromPhone; // For Twilio or Meta

    @Column(name = "auto_send_daily", nullable = false)
    @Builder.Default
    private Boolean autoSendDaily = true;

    @Column(name = "pastor_signature")
    @Builder.Default
    private String pastorSignature = "Pastor & GraceConnect Church Family";

    @Column(name = "last_run_date")
    private LocalDate lastRunDate;
}
