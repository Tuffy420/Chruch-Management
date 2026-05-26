package com.graceconnect.church.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JwtResponse {
    private String token;
    
    @Builder.Default
    private String type = "Bearer";
    private String email;
    private String role;
    private String fullName;
}
