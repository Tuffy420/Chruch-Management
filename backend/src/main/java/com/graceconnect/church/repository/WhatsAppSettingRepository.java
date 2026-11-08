package com.graceconnect.church.repository;

import com.graceconnect.church.model.WhatsAppSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WhatsAppSettingRepository extends JpaRepository<WhatsAppSetting, Long> {
    Optional<WhatsAppSetting> findFirstByOrderByIdAsc();
}
