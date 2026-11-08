package com.graceconnect.church.repository;

import com.graceconnect.church.model.WhatsAppLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface WhatsAppLogRepository extends JpaRepository<WhatsAppLog, Long> {

    Page<WhatsAppLog> findAllByOrderBySentAtDesc(Pageable pageable);

    List<WhatsAppLog> findBySentAtBetweenOrderBySentAtDesc(LocalDateTime start, LocalDateTime end);

    boolean existsByPhoneNumberAndOccasionAndSentAtBetween(
            String phoneNumber, String occasion, LocalDateTime start, LocalDateTime end);
}
