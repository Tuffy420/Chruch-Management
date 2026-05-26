package com.graceconnect.church.repository;

import com.graceconnect.church.model.ActivityLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {
    List<ActivityLog> findAllByOrderByTimestampDesc(Pageable pageable);

    @Query("SELECT a FROM ActivityLog a WHERE LOWER(a.action) NOT LIKE 'user login successful%'")
    List<ActivityLog> findRecentActivitiesExcludingLogins(Pageable pageable);
}
