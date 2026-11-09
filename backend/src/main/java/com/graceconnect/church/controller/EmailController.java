package com.graceconnect.church.controller;

import com.graceconnect.church.dto.ApiResponse;
import com.graceconnect.church.dto.CelebrationItemDto;
import com.graceconnect.church.dto.EmailSendResultDto;
import com.graceconnect.church.dto.EmailTestRequest;
import com.graceconnect.church.model.EmailLog;
import com.graceconnect.church.model.EmailSetting;
import com.graceconnect.church.service.EmailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/email")
@Tag(name = "Email Blessings Module", description = "Endpoints for managing automated scripture birthday and wedding anniversary email blessings, SMTP configuration, and audit logs.")
public class EmailController {

    private final EmailService emailService;

    public EmailController(EmailService emailService) {
        this.emailService = emailService;
    }

    @GetMapping("/celebrations-today")
    @Operation(summary = "Get Today's Celebrations with Email Blessings", description = "Retrieves all members celebrating birthdays and wedding anniversaries today with personalized scriptures and delivery status.")
    public ResponseEntity<ApiResponse<List<CelebrationItemDto>>> getTodayCelebrations() {
        List<CelebrationItemDto> items = emailService.getCelebrationsForToday();
        return ResponseEntity.ok(ApiResponse.success("Today's celebrations retrieved successfully", items));
    }

    @PostMapping("/auto-dispatch-today")
    @Operation(summary = "Run Daily Email Auto-Dispatch", description = "Triggers automated email dispatch of scripture blessings to all members celebrating today.")
    public ResponseEntity<ApiResponse<EmailSendResultDto>> autoDispatchToday(Authentication authentication) {
        String user = authentication != null ? authentication.getName() : "Administrator";
        EmailSendResultDto result = emailService.autoDispatchToday(user);
        return ResponseEntity.ok(ApiResponse.success("Email auto-dispatch executed", result));
    }

    @PostMapping("/send-single")
    @Operation(summary = "Send Single Member Email Blessing", description = "Dispatches a personalized scripture blessing email to a specific member ID.")
    public ResponseEntity<ApiResponse<EmailLog>> sendSingle(
            @RequestParam Long memberId,
            @RequestParam(defaultValue = "BIRTHDAY") String occasion,
            Authentication authentication) {
        String user = authentication != null ? authentication.getName() : "Administrator";
        EmailLog log = emailService.sendSingleCelebration(memberId, occasion, user);
        return ResponseEntity.ok(ApiResponse.success("Email blessing sent successfully", log));
    }

    @GetMapping("/config")
    @Operation(summary = "Get Email / SMTP Settings", description = "Returns active SMTP server host, port, sender email, and automated schedule status.")
    public ResponseEntity<ApiResponse<EmailSetting>> getConfig() {
        return ResponseEntity.ok(ApiResponse.success("Email settings retrieved", emailService.getSettings()));
    }

    @PostMapping("/config")
    @Operation(summary = "Update Email / SMTP Settings", description = "Updates SMTP host, port, username, password, sender identity, and daily auto-send toggle.")
    public ResponseEntity<ApiResponse<EmailSetting>> updateConfig(@RequestBody EmailSetting updated) {
        return ResponseEntity.ok(ApiResponse.success("Email settings updated successfully", emailService.saveSettings(updated)));
    }

    @PostMapping("/test-send")
    @Operation(summary = "Send Test Verification Email", description = "Sends a verification email to confirm that SMTP credentials are functional.")
    public ResponseEntity<ApiResponse<EmailLog>> testSend(
            @RequestBody EmailTestRequest request,
            Authentication authentication) {
        String user = authentication != null ? authentication.getName() : "Administrator";
        EmailLog log = emailService.testSend(request, user);
        return ResponseEntity.ok(ApiResponse.success("Test email dispatched", log));
    }

    @GetMapping("/logs")
    @Operation(summary = "Get Email Delivery Logs", description = "Returns paginated list of all automated and manual email blessings dispatched.")
    public ResponseEntity<ApiResponse<Page<EmailLog>>> getLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<EmailLog> logs = emailService.getLogs(PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success("Email delivery logs retrieved", logs));
    }
}

