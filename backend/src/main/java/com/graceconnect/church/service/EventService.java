package com.graceconnect.church.service;

import com.graceconnect.church.exception.ResourceNotFoundException;
import com.graceconnect.church.model.ActivityLog;
import com.graceconnect.church.model.Event;
import com.graceconnect.church.repository.ActivityLogRepository;
import com.graceconnect.church.repository.EventRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class EventService {

    private final EventRepository eventRepository;
    private final ActivityLogRepository activityLogRepository;

    public EventService(EventRepository eventRepository, ActivityLogRepository activityLogRepository) {
        this.eventRepository = eventRepository;
        this.activityLogRepository = activityLogRepository;
    }

    private String getCurrentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null) ? auth.getName() : "System Admin";
    }

    @Transactional(readOnly = true)
    public Page<Event> getEvents(String category, Pageable pageable) {
        if (category != null && !category.trim().isEmpty()) {
            return eventRepository.findByCategoryContainingIgnoreCase(category.trim(), pageable);
        }
        return eventRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public List<Event> getEventsBetween(LocalDate start, LocalDate end) {
        return eventRepository.findByDateBetween(start, end);
    }

    @Transactional(readOnly = true)
    public List<Event> getUpcomingEvents() {
        return eventRepository.findByDateGreaterThanEqualOrderByDateAsc(LocalDate.now());
    }

    @Transactional(readOnly = true)
    public Event getEventById(Long id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Scheduled event not found with ID: " + id));
    }

    @Transactional
    public Event createEvent(Event event) {
        Event saved = eventRepository.save(event);

        // Audit Log
        activityLogRepository.save(ActivityLog.builder()
                .action("Scheduled function \"" + saved.getTitle() + "\" added to calendar")
                .module("Schedule")
                .performedBy(getCurrentUser())
                .timestamp(LocalDateTime.now())
                .build());

        return saved;
    }

    @Transactional
    public Event updateEvent(Long id, Event eventDetails) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Scheduled event not found with ID: " + id));

        event.setTitle(eventDetails.getTitle());
        event.setDate(eventDetails.getDate());
        event.setTime(eventDetails.getTime());
        event.setLocation(eventDetails.getLocation());
        event.setCategory(eventDetails.getCategory());

        Event saved = eventRepository.save(event);

        // Audit Log
        activityLogRepository.save(ActivityLog.builder()
                .action("Scheduled function \"" + saved.getTitle() + "\" details updated")
                .module("Schedule")
                .performedBy(getCurrentUser())
                .timestamp(LocalDateTime.now())
                .build());

        return saved;
    }

    @Transactional
    public void deleteEvent(Long id) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Scheduled event not found with ID: " + id));

        String title = event.getTitle();
        eventRepository.delete(event);

        // Audit Log
        activityLogRepository.save(ActivityLog.builder()
                .action("Scheduled function \"" + title + "\" deleted from calendar")
                .module("Schedule")
                .performedBy(getCurrentUser())
                .timestamp(LocalDateTime.now())
                .build());
    }
}
