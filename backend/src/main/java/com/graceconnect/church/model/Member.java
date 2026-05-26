package com.graceconnect.church.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "members")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "family")
@EqualsAndHashCode(exclude = "family")
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "gender")
    private String gender;

    @Column(name = "dob")
    private LocalDate dob;

    @Column(name = "mobile_number")
    private String mobileNumber;

    @Column(name = "email")
    private String email;

    @Column(name = "occupation")
    private String occupation;

    @Column(name = "marriage_status")
    private String marriageStatus; // Single, Married, Widowed

    @Column(name = "marriage_date")
    private LocalDate marriageDate;

    @Column(name = "baptism_status")
    private String baptismStatus; // Baptized, Unbaptized
    
    @Column(name = "profile_photo", columnDefinition = "TEXT")
    private String profilePhoto; // URL or base64 placeholder

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "family_id")
    @JsonBackReference
    private Family family;
}
