package com.graceconnect.church.repository;

import com.graceconnect.church.model.EmailSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmailSettingRepository extends JpaRepository<EmailSetting, Long> {
    Optional<EmailSetting> findFirstByOrderByIdAsc();
}
