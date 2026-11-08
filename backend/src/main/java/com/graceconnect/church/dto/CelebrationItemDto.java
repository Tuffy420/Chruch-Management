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
public class CelebrationItemDto {
    private Long memberId;
    private String fullName;
    private String mobileNumber;
    private String profilePhoto;
    private String familyName;
    private String occasion; // BIRTHDAY or ANNIVERSARY
    private LocalDate celebrationDate;
    private Integer milestoneYears; // Age or Years of Marriage
    private String scriptureRef;
    private String scriptureText;
    private String devotionalReflection;
    private String fullMessage;
    private String status; // SENT, READY
    private String lastSentAt;
}
