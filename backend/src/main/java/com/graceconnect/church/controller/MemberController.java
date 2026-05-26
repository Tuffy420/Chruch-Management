package com.graceconnect.church.controller;

import com.graceconnect.church.dto.ApiResponse;
import com.graceconnect.church.dto.MemberDto;
import com.graceconnect.church.service.MemberService;
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
@RequestMapping("/api/members")
@Tag(name = "Member Management Module", description = "Endpoints to create, list, search, update, and delete individual church members.")
public class MemberController {

    private final MemberService memberService;

    public MemberController(MemberService memberService) {
        this.memberService = memberService;
    }

    @GetMapping
    @Operation(summary = "Get, Search & Paginate Members roster", description = "Fetches list of registered congregation members. Supports optional text search query, page indexing, capacity limits, and custom sorting.")
    public ResponseEntity<ApiResponse<Page<MemberDto>>> getMembers(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "firstName") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {

        Sort.Direction direction = Sort.Direction.fromString(sortDir.toLowerCase());
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        Page<MemberDto> result = memberService.getMembers(search, pageable);
        return ResponseEntity.ok(ApiResponse.success("Members roster retrieved successfully", result));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get member detail profile", description = "Retrieves active fields of a single congregation member profile.")
    public ResponseEntity<ApiResponse<MemberDto>> getMemberById(@PathVariable Long id) {
        MemberDto result = memberService.getMemberById(id);
        return ResponseEntity.ok(ApiResponse.success("Member profile details retrieved successfully", result));
    }

    @PostMapping
    @Operation(summary = "Register a new member on the roster", description = "Adds a new individual member under a specific family household reference.")
    public ResponseEntity<ApiResponse<MemberDto>> createMember(@Valid @RequestBody MemberDto dto) {
        MemberDto result = memberService.createMember(dto);
        return ResponseEntity.ok(ApiResponse.success("Member registered successfully on roster", result));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update member profile details", description = "Modifies parameters of an existing member profile, including shifting family associations.")
    public ResponseEntity<ApiResponse<MemberDto>> updateMember(@PathVariable Long id, @Valid @RequestBody MemberDto dto) {
        MemberDto result = memberService.updateMember(id, dto);
        return ResponseEntity.ok(ApiResponse.success("Member profile details updated successfully", result));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove member from active roster", description = "Removes member profile entry completely from database rosters.")
    public ResponseEntity<ApiResponse<Void>> deleteMember(@PathVariable Long id) {
        memberService.deleteMember(id);
        return ResponseEntity.ok(ApiResponse.success("Member profile removed from active rosters successfully"));
    }
}
