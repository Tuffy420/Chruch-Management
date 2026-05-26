package com.graceconnect.church.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberDto {
    private Long id;
    private String firstName;
    private String lastName;
    private String gender;
    private LocalDate dob;
    private String mobileNumber;
    private String email;
    private String occupation;
    private String marriageStatus;
    private LocalDate marriageDate;
    private String baptismStatus;
    private String profilePhoto;
    private Long familyId;
    private String familyName;
}
