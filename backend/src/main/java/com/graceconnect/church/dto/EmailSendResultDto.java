package com.graceconnect.church.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailSendResultDto {
    private int totalFound;
    private int successfullySent;
    private int failed;
    private int alreadySentToday;
    private String summaryMessage;
    private List<String> dispatchedNames;
}
