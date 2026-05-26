package com.graceconnect.church.controller;

import com.graceconnect.church.dto.ApiResponse;
import com.graceconnect.church.model.Account;
import com.graceconnect.church.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/accounts")
@Tag(name = "Financial Accounting Module", description = "Endpoints for recording general tithes, missionary outreach offerings, and general parish expenses.")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping
    @Operation(summary = "Get & Paginate Financial Ledger transactions", description = "Fetches general income and expense records. Supports optional filtering by transaction type.")
    public ResponseEntity<ApiResponse<Page<Account>>> getTransactions(
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "date") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort.Direction direction = Sort.Direction.fromString(sortDir.toLowerCase());
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        Page<Account> result = accountService.getTransactions(type, pageable);
        return ResponseEntity.ok(ApiResponse.success("Financial ledger retrieved successfully", result));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get transaction details", description = "Retrieves parameters of a single financial ledger record.")
    public ResponseEntity<ApiResponse<Account>> getTransactionById(@PathVariable Long id) {
        Account result = accountService.getTransactionById(id);
        return ResponseEntity.ok(ApiResponse.success("Transaction loaded successfully", result));
    }

    @PostMapping
    @Operation(summary = "Record a new ledger transaction", description = "Inserts a new transaction log representing a parish offering income or building/operating expense.")
    public ResponseEntity<ApiResponse<Account>> createTransaction(@Valid @RequestBody Account transaction) {
        Account result = accountService.createTransaction(transaction);
        return ResponseEntity.ok(ApiResponse.success("Transaction recorded successfully on ledger", result));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update transaction record", description = "Modifies active properties of a previously logged ledger transaction.")
    public ResponseEntity<ApiResponse<Account>> updateTransaction(@PathVariable Long id, @Valid @RequestBody Account transactionDetails) {
        Account result = accountService.updateTransaction(id, transactionDetails);
        return ResponseEntity.ok(ApiResponse.success("Transaction details modified successfully", result));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete transaction from ledger", description = "Completely purges a transaction entry from database records.")
    public ResponseEntity<ApiResponse<Void>> deleteTransaction(@PathVariable Long id) {
        accountService.deleteTransaction(id);
        return ResponseEntity.ok(ApiResponse.success("Transaction deleted from ledger successfully"));
    }
}
