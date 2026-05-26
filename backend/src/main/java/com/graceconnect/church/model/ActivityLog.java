package com.graceconnect.church.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "activity_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActivityLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "action", nullable = false)
    private String action;

    @Column(name = "module")
    private String module; // Family, Member, Offering, Schedule, System

    @Column(name = "performed_by")
    private String performedBy;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;
}
