package com.graceconnect.church.repository;

import com.graceconnect.church.model.EmailLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface EmailLogRepository extends JpaRepository<EmailLog, Long> {

    Page<EmailLog> findAllByOrderBySentAtDesc(Pageable pageable);

    boolean existsByRecipientEmailAndOccasionAndSentAtBetween(
            String recipientEmail, String occasion, LocalDateTime start, LocalDateTime end);
}

