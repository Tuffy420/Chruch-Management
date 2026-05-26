package com.graceconnect.church.controller;

import com.graceconnect.church.dto.ApiResponse;
import com.graceconnect.church.dto.FamilyDto;
import com.graceconnect.church.service.FamilyService;
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
@RequestMapping("/api/families")
@Tag(name = "Family Management Module", description = "Endpoints to create, list, search, update, and delete church household families.")
public class FamilyController {

    private final FamilyService familyService;

    public FamilyController(FamilyService familyService) {
        this.familyService = familyService;
    }

    @GetMapping
    @Operation(summary = "Get, Search & Paginate Families list", description = "Fetches list of registered family households. Supports optional text search query, page indexing, capacity limits, and custom sort sorting directions.")
    public ResponseEntity<ApiResponse<Page<FamilyDto>>> getFamilies(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {
        
        Sort.Direction direction = Sort.Direction.fromString(sortDir.toLowerCase());
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        
        Page<FamilyDto> result = familyService.getFamilies(search, pageable);
        return ResponseEntity.ok(ApiResponse.success("Families list retrieved successfully", result));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get detailed family profile", description = "Retrieves complete attributes of a specific family, along with flat details of all members linked to the household.")
    public ResponseEntity<ApiResponse<FamilyDto>> getFamilyById(@PathVariable Long id) {
        FamilyDto result = familyService.getFamilyById(id);
        return ResponseEntity.ok(ApiResponse.success("Family details retrieved successfully", result));
    }

    @PostMapping
    @Operation(summary = "Create a new family household record", description = "Registers a new family household unit inside the system directory.")
    public ResponseEntity<ApiResponse<FamilyDto>> createFamily(@Valid @RequestBody FamilyDto dto) {
        FamilyDto result = familyService.createFamily(dto);
        return ResponseEntity.ok(ApiResponse.success("Family household registered successfully", result));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update family household details", description = "Modifies active properties of an existing family household profile.")
    public ResponseEntity<ApiResponse<FamilyDto>> updateFamily(@PathVariable Long id, @Valid @RequestBody FamilyDto dto) {
        FamilyDto result = familyService.updateFamily(id, dto);
        return ResponseEntity.ok(ApiResponse.success("Family household details updated successfully", result));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete family household from directory", description = "Deletes family entry and cleans up associated database records.")
    public ResponseEntity<ApiResponse<Void>> deleteFamily(@PathVariable Long id) {
        familyService.deleteFamily(id);
        return ResponseEntity.ok(ApiResponse.success("Family household removed from system directories successfully"));
    }
}
