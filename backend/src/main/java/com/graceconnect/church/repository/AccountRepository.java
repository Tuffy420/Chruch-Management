package com.graceconnect.church.repository;

import com.graceconnect.church.model.Account;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {
    
    Page<Account> findByType(String type, Pageable pageable);
    
    List<Account> findByDateBetween(LocalDate startDate, LocalDate endDate);

    List<Account> findByType(String type);

    List<Account> findByTypeAndDateBetween(String type, LocalDate startDate, LocalDate endDate);
}
