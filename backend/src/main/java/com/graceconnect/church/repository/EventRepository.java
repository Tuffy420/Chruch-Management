package com.graceconnect.church.repository;

import com.graceconnect.church.model.Event;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {
    List<Event> findByDateBetween(LocalDate startDate, LocalDate endDate);
    List<Event> findByDateGreaterThanEqualOrderByDateAsc(LocalDate date);
    Page<Event> findByCategoryContainingIgnoreCase(String category, Pageable pageable);
}
