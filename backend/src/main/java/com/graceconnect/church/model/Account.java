package com.graceconnect.church.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "accounts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "type", nullable = false)
    private String type; // INCOME, EXPENSE

    @Column(name = "member_name")
    private String memberName; // Optional (mainly for donations/tithes)

    @Column(name = "amount", nullable = false)
    private Double amount;

    @Column(name = "fund")
    private String fund; // General Fund, Missions & Outreach, Building Maintenance, Charity & Welfare

    @Column(name = "notes")
    private String notes;

    @Column(name = "date", nullable = false)
    private LocalDate date;
}
