package com.graceconnect.church.service;

import com.graceconnect.church.dto.FamilyDto;
import com.graceconnect.church.dto.MemberDto;
import com.graceconnect.church.exception.ResourceNotFoundException;
import com.graceconnect.church.model.ActivityLog;
import com.graceconnect.church.model.Family;
import com.graceconnect.church.model.Member;
import com.graceconnect.church.repository.ActivityLogRepository;
import com.graceconnect.church.repository.FamilyRepository;
import com.graceconnect.church.repository.MemberRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class FamilyService {

    private final FamilyRepository familyRepository;
    private final MemberRepository memberRepository;
    private final ActivityLogRepository activityLogRepository;

    public FamilyService(FamilyRepository familyRepository, MemberRepository memberRepository, ActivityLogRepository activityLogRepository) {
        this.familyRepository = familyRepository;
        this.memberRepository = memberRepository;
        this.activityLogRepository = activityLogRepository;
    }

    private String getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null) ? auth.getName() : "System Admin";
    }

    public FamilyDto convertToDto(Family family) {
        if (family == null) return null;
        
        List<MemberDto> memberDtos = null;
        if (family.getMembers() != null) {
            memberDtos = family.getMembers().stream()
                    .map(m -> MemberDto.builder()
                            .id(m.getId())
                            .firstName(m.getFirstName())
                            .lastName(m.getLastName())
                            .gender(m.getGender())
                            .dob(m.getDob())
                            .mobileNumber(m.getMobileNumber())
                            .email(m.getEmail())
                            .occupation(m.getOccupation())
                            .marriageStatus(m.getMarriageStatus())
                            .baptismStatus(m.getBaptismStatus())
                            .profilePhoto(m.getProfilePhoto())
                            .familyId(family.getId())
                            .familyName(family.getName())
                            .build())
                    .collect(Collectors.toList());
        }

        return FamilyDto.builder()
                .id(family.getId())
                .name(family.getName())
                .address(family.getAddress())
                .email(family.getEmail())
                .phone(family.getPhone())
                .dateRegistered(family.getDateRegistered())
                .members(memberDtos)
                .build();
    }

    @Transactional(readOnly = true)
    public Page<FamilyDto> getFamilies(String search, Pageable pageable) {
        Page<Family> families;
        if (search != null && !search.trim().isEmpty()) {
            families = familyRepository.searchFamilies(search.trim(), pageable);
        } else {
            families = familyRepository.findAll(pageable);
        }
        return families.map(this::convertToDto);
    }

    @Transactional(readOnly = true)
    public FamilyDto getFamilyById(Long id) {
        Family family = familyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Family household not found with ID: " + id));
        return convertToDto(family);
    }

    @Transactional
    public FamilyDto createFamily(FamilyDto dto) {
        Family family = Family.builder()
                .name(dto.getName())
                .address(dto.getAddress())
                .email(dto.getEmail())
                .phone(dto.getPhone())
                .dateRegistered(dto.getDateRegistered() != null ? dto.getDateRegistered() : LocalDate.now())
                .build();

        Family saved = familyRepository.save(family);

        // Audit Log
        activityLogRepository.save(ActivityLog.builder()
                .action(saved.getName() + " registered successfully")
                .module("Family")
                .performedBy(getCurrentUser())
                .timestamp(LocalDateTime.now())
                .build());

        return convertToDto(saved);
    }

    @Transactional
    public FamilyDto updateFamily(Long id, FamilyDto dto) {
        Family family = familyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Family household not found with ID: " + id));

        family.setName(dto.getName());
        family.setAddress(dto.getAddress());
        family.setEmail(dto.getEmail());
        family.setPhone(dto.getPhone());
        if (dto.getDateRegistered() != null) {
            family.setDateRegistered(dto.getDateRegistered());
        }

        Family saved = familyRepository.save(family);

        // Audit Log
        activityLogRepository.save(ActivityLog.builder()
                .action(saved.getName() + " profile updated")
                .module("Family")
                .performedBy(getCurrentUser())
                .timestamp(LocalDateTime.now())
                .build());

        return convertToDto(saved);
    }

    @Transactional
    public void deleteFamily(Long id) {
        Family family = familyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Family household not found with ID: " + id));

        String familyName = family.getName();
        
        // Manually cascadingly delete linked member documents
        List<Member> familyMembers = memberRepository.findByFamilyId(id);
        memberRepository.deleteAll(familyMembers);
        
        familyRepository.delete(family);

        // Audit Log
        activityLogRepository.save(ActivityLog.builder()
                .action(familyName + " completely deleted from roster")
                .module("Family")
                .performedBy(getCurrentUser())
                .timestamp(LocalDateTime.now())
                .build());
    }
}
