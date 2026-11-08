package com.graceconnect.church.controller;

import com.graceconnect.church.dto.ApiResponse;
import com.graceconnect.church.dto.CelebrationItemDto;
import com.graceconnect.church.dto.WhatsAppSendResultDto;
import com.graceconnect.church.dto.WhatsAppTestRequest;
import com.graceconnect.church.model.WhatsAppLog;
import com.graceconnect.church.model.WhatsAppSetting;
import com.graceconnect.church.service.WhatsAppService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/whatsapp")
@Tag(name = "WhatsApp Automation Module", description = "Endpoints for managing automated scripture birthday and wedding anniversary wishes, gateway integration, and delivery logs.")
public class WhatsAppController {

    private final WhatsAppService whatsAppService;

    public WhatsAppController(WhatsAppService whatsAppService) {
        this.whatsAppService = whatsAppService;
    }

    @GetMapping("/celebrations-today")
    @Operation(summary = "Get Today's Celebrations", description = "Retrieves all members celebrating birthdays and wedding anniversaries today, formatted with scripture verses and delivery status.")
    public ResponseEntity<ApiResponse<List<CelebrationItemDto>>> getTodayCelebrations() {
        List<CelebrationItemDto> items = whatsAppService.getCelebrationsForToday();
        return ResponseEntity.ok(ApiResponse.success("Today's celebrations retrieved successfully", items));
    }

    @PostMapping("/auto-dispatch-today")
    @Operation(summary = "Run Auto-Dispatch for Today", description = "Triggers automated WhatsApp sending for all members celebrating birthdays and wedding anniversaries today.")
    public ResponseEntity<ApiResponse<WhatsAppSendResultDto>> autoDispatchToday(Authentication authentication) {
        String user = authentication != null ? authentication.getName() : "Administrator";
        WhatsAppSendResultDto result = whatsAppService.autoDispatchToday(user);
        return ResponseEntity.ok(ApiResponse.success("Auto-dispatch executed successfully", result));
    }

    @PostMapping("/send-single")
    @Operation(summary = "Send Single WhatsApp Wish", description = "Dispatches a personalized scripture blessing to a single church member via WhatsApp API.")
    public ResponseEntity<ApiResponse<WhatsAppLog>> sendSingle(
            @RequestParam Long memberId,
            @RequestParam(defaultValue = "BIRTHDAY") String occasion,
            Authentication authentication) {
        String user = authentication != null ? authentication.getName() : "Administrator";
        WhatsAppLog log = whatsAppService.sendSingleCelebration(memberId, occasion, user);
        return ResponseEntity.ok(ApiResponse.success("WhatsApp blessing dispatched successfully", log));
    }

    @GetMapping("/config")
    @Operation(summary = "Get WhatsApp Gateway Settings", description = "Returns active WhatsApp provider configuration, instance credentials, and automated schedule status.")
    public ResponseEntity<ApiResponse<WhatsAppSetting>> getConfig() {
        return ResponseEntity.ok(ApiResponse.success("WhatsApp settings retrieved", whatsAppService.getSettings()));
    }

    @PostMapping("/config")
    @Operation(summary = "Update WhatsApp Gateway Settings", description = "Updates WhatsApp API gateway URL, tokens, instance IDs, pastor signature, and automated cron settings.")
    public ResponseEntity<ApiResponse<WhatsAppSetting>> updateConfig(@RequestBody WhatsAppSetting updated) {
        return ResponseEntity.ok(ApiResponse.success("WhatsApp settings updated successfully", whatsAppService.saveSettings(updated)));
    }

    @PostMapping("/test-send")
    @Operation(summary = "Send Test WhatsApp Message", description = "Sends a verification message to a given phone number to confirm API credentials.")
    public ResponseEntity<ApiResponse<WhatsAppLog>> testSend(
            @RequestBody WhatsAppTestRequest request,
            Authentication authentication) {
        String user = authentication != null ? authentication.getName() : "Administrator";
        WhatsAppLog log = whatsAppService.testSend(request, user);
        return ResponseEntity.ok(ApiResponse.success("Test message dispatched", log));
    }

    @GetMapping("/logs")
    @Operation(summary = "Get WhatsApp Delivery Audit Logs", description = "Returns paginated list of all automated and manual WhatsApp messages dispatched with timestamps and statuses.")
    public ResponseEntity<ApiResponse<Page<WhatsAppLog>>> getLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<WhatsAppLog> logs = whatsAppService.getLogs(PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success("WhatsApp audit logs retrieved", logs));
    }
}
