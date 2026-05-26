package com.graceconnect.church.service;

import com.graceconnect.church.exception.ResourceNotFoundException;
import com.graceconnect.church.model.Account;
import com.graceconnect.church.model.ActivityLog;
import com.graceconnect.church.repository.AccountRepository;
import com.graceconnect.church.repository.ActivityLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final ActivityLogRepository activityLogRepository;

    public AccountService(AccountRepository accountRepository, ActivityLogRepository activityLogRepository) {
        this.accountRepository = accountRepository;
        this.activityLogRepository = activityLogRepository;
    }

    private String getCurrentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null) ? auth.getName() : "System Admin";
    }

    @Transactional(readOnly = true)
    public Page<Account> getTransactions(String type, Pageable pageable) {
        if (type != null && !type.trim().isEmpty()) {
            return accountRepository.findByType(type.trim().toUpperCase(), pageable);
        }
        return accountRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Account getTransactionById(Long id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction record not found with ID: " + id));
    }

    @Transactional
    public Account createTransaction(Account transaction) {
        transaction.setType(transaction.getType().toUpperCase());
        if (transaction.getDate() == null) {
            transaction.setDate(LocalDate.now());
        }
        Account saved = accountRepository.save(transaction);

        // Audit Log
        String logMessage = saved.getType().equals("INCOME") ?
                "Tithe/Offering of ₹" + saved.getAmount() + " received from " + saved.getMemberName() :
                "Expense of ₹" + saved.getAmount() + " paid out for " + saved.getNotes();

        activityLogRepository.save(ActivityLog.builder()
                .action(logMessage)
                .module("Offering")
                .performedBy(getCurrentUser())
                .timestamp(LocalDateTime.now())
                .build());

        return saved;
    }

    @Transactional
    public Account updateTransaction(Long id, Account transactionDetails) {
        Account t = accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction record not found with ID: " + id));

        t.setType(transactionDetails.getType().toUpperCase());
        t.setMemberName(transactionDetails.getMemberName());
        t.setAmount(transactionDetails.getAmount());
        t.setFund(transactionDetails.getFund());
        t.setNotes(transactionDetails.getNotes());
        t.setDate(transactionDetails.getDate());

        Account saved = accountRepository.save(t);

        // Audit Log
        activityLogRepository.save(ActivityLog.builder()
                .action("Transaction record modified: ID " + saved.getId())
                .module("Offering")
                .performedBy(getCurrentUser())
                .timestamp(LocalDateTime.now())
                .build());

        return saved;
    }

    @Transactional
    public void deleteTransaction(Long id) {
        Account t = accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction record not found with ID: " + id));

        double amt = t.getAmount();
        String type = t.getType();
        accountRepository.delete(t);

        // Audit Log
        activityLogRepository.save(ActivityLog.builder()
                .action("Transaction (" + type + " of ₹" + amt + ") deleted from ledger")
                .module("Offering")
                .performedBy(getCurrentUser())
                .timestamp(LocalDateTime.now())
                .build());
    }
}
