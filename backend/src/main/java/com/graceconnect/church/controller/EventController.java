package com.graceconnect.church.controller;

import com.graceconnect.church.dto.ApiResponse;
import com.graceconnect.church.model.Event;
import com.graceconnect.church.service.EventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/events")
@Tag(name = "Event & Functions Module", description = "Endpoints for managing community services, youth fellowships, and calendars.")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @GetMapping
    @Operation(summary = "Get & Paginate Event list", description = "Retrieves calendar items. Supports category-based filtering, sorting, and pagination.")
    public ResponseEntity<ApiResponse<Page<Event>>> getEvents(
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "date") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {

        Sort.Direction direction = Sort.Direction.fromString(sortDir.toLowerCase());
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        Page<Event> result = eventService.getEvents(category, pageable);
        return ResponseEntity.ok(ApiResponse.success("Events schedule retrieved successfully", result));
    }

    @GetMapping("/calendar")
    @Operation(summary = "Get calendar items in date range", description = "Queries scheduled services and functions between a start and end date boundary for calendar displays.")
    public ResponseEntity<ApiResponse<List<Event>>> getEventsBetween(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        
        List<Event> result = eventService.getEventsBetween(start, end);
        return ResponseEntity.ok(ApiResponse.success("Calendar events loaded successfully", result));
    }

    @GetMapping("/upcoming")
    @Operation(summary = "Get all upcoming events", description = "Retrieves chronological list of events scheduled starting from current local date.")
    public ResponseEntity<ApiResponse<List<Event>>> getUpcomingEvents() {
        List<Event> result = eventService.getUpcomingEvents();
        return ResponseEntity.ok(ApiResponse.success("Upcoming events retrieved successfully", result));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get scheduled event details", description = "Retrieves parameters of a single scheduled event profile.")
    public ResponseEntity<ApiResponse<Event>> getEventById(@PathVariable Long id) {
        Event result = eventService.getEventById(id);
        return ResponseEntity.ok(ApiResponse.success("Scheduled event loaded successfully", result));
    }

    @PostMapping
    @Operation(summary = "Schedule a new event on calendar", description = "Locks a new event item inside the central community calendar system.")
    public ResponseEntity<ApiResponse<Event>> createEvent(@Valid @RequestBody Event event) {
        Event result = eventService.createEvent(event);
        return ResponseEntity.ok(ApiResponse.success("Event scheduled on calendar successfully", result));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update scheduled event properties", description = "Modifies times, locations, and descriptions of an active event schedule.")
    public ResponseEntity<ApiResponse<Event>> updateEvent(@PathVariable Long id, @Valid @RequestBody Event eventDetails) {
        Event result = eventService.updateEvent(id, eventDetails);
        return ResponseEntity.ok(ApiResponse.success("Scheduled event updated successfully", result));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove scheduled event from calendar", description = "Deletes and cleans up event locks from calendar registries.")
    public ResponseEntity<ApiResponse<Void>> deleteEvent(@PathVariable Long id) {
        eventService.deleteEvent(id);
        return ResponseEntity.ok(ApiResponse.success("Event removed from calendar schedules successfully"));
    }
}
