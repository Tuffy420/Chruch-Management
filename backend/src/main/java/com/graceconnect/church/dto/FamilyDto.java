package com.graceconnect.church.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FamilyDto {
    private Long id;
    private String name;
    private String address;
    private String email;
    private String phone;
    private LocalDate dateRegistered;
    private List<MemberDto> members;
}
