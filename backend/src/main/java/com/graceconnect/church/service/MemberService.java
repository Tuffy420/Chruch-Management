package com.graceconnect.church.service;

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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class MemberService {

    private final MemberRepository memberRepository;
    private final FamilyRepository familyRepository;
    private final ActivityLogRepository activityLogRepository;

    public MemberService(MemberRepository memberRepository, FamilyRepository familyRepository,
                         ActivityLogRepository activityLogRepository) {
        this.memberRepository = memberRepository;
        this.familyRepository = familyRepository;
        this.activityLogRepository = activityLogRepository;
    }

    private String getCurrentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null) ? auth.getName() : "System Admin";
    }

    public MemberDto convertToDto(Member member) {
        if (member == null) return null;
        
        Long familyId = null;
        String familyName = null;
        if (member.getFamily() != null) {
            familyId = member.getFamily().getId();
            familyName = member.getFamily().getName();
        }

        return MemberDto.builder()
                .id(member.getId())
                .firstName(member.getFirstName())
                .lastName(member.getLastName())
                .gender(member.getGender())
                .dob(member.getDob())
                .mobileNumber(member.getMobileNumber())
                .email(member.getEmail())
                .occupation(member.getOccupation())
                .marriageStatus(member.getMarriageStatus())
                .marriageDate(member.getMarriageDate())
                .baptismStatus(member.getBaptismStatus())
                .profilePhoto(member.getProfilePhoto())
                .familyId(familyId)
                .familyName(familyName)
                .build();
    }

    @Transactional(readOnly = true)
    public Page<MemberDto> getMembers(String search, Pageable pageable) {
        Page<Member> members;
        if (search != null && !search.trim().isEmpty()) {
            members = memberRepository.searchMembers(search.trim(), pageable);
        } else {
            members = memberRepository.findAll(pageable);
        }
        return members.map(this::convertToDto);
    }

    @Transactional(readOnly = true)
    public MemberDto getMemberById(Long id) {
        Member m = memberRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found on roster with ID: " + id));
        return convertToDto(m);
    }

    @Transactional
    public MemberDto createMember(MemberDto dto) {
        Family family = null;
        if (dto.getFamilyId() != null) {
            family = familyRepository.findById(dto.getFamilyId())
                    .orElseThrow(() -> new ResourceNotFoundException("Linked Family Household not found with ID: " + dto.getFamilyId()));
        }

        Member member = Member.builder()
                .firstName(dto.getFirstName())
                .lastName(dto.getLastName())
                .gender(dto.getGender())
                .dob(dto.getDob())
                .mobileNumber(dto.getMobileNumber())
                .email(dto.getEmail())
                .occupation(dto.getOccupation())
                .marriageStatus(dto.getMarriageStatus())
                .marriageDate(dto.getMarriageDate())
                .baptismStatus(dto.getBaptismStatus())
                .profilePhoto(dto.getProfilePhoto())
                .family(family)
                .build();

        Member saved = memberRepository.save(member);

        // Audit Log
        activityLogRepository.save(ActivityLog.builder()
                .action(saved.getFirstName() + " " + saved.getLastName() + " added to congregation roster")
                .module("Member")
                .performedBy(getCurrentUser())
                .timestamp(LocalDateTime.now())
                .build());

        return convertToDto(saved);
    }

    @Transactional
    public MemberDto updateMember(Long id, MemberDto dto) {
        Member member = memberRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found on roster with ID: " + id));

        member.setFirstName(dto.getFirstName());
        member.setLastName(dto.getLastName());
        member.setGender(dto.getGender());
        member.setDob(dto.getDob());
        member.setMobileNumber(dto.getMobileNumber());
        member.setEmail(dto.getEmail());
        member.setOccupation(dto.getOccupation());
        member.setMarriageStatus(dto.getMarriageStatus());
        member.setMarriageDate(dto.getMarriageDate());
        member.setBaptismStatus(dto.getBaptismStatus());
        member.setProfilePhoto(dto.getProfilePhoto());

        if (dto.getFamilyId() != null) {
            Family family = familyRepository.findById(dto.getFamilyId())
                    .orElseThrow(() -> new ResourceNotFoundException("Linked Family Household not found with ID: " + dto.getFamilyId()));
            member.setFamily(family);
        } else {
            member.setFamily(null);
        }

        Member saved = memberRepository.save(member);

        // Audit Log
        activityLogRepository.save(ActivityLog.builder()
                .action(saved.getFirstName() + " " + saved.getLastName() + " details updated")
                .module("Member")
                .performedBy(getCurrentUser())
                .timestamp(LocalDateTime.now())
                .build());

        return convertToDto(saved);
    }

    @Transactional
    public void deleteMember(Long id) {
        Member member = memberRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found on roster with ID: " + id));

        String fullName = member.getFirstName() + " " + member.getLastName();
        memberRepository.delete(member);

        // Audit Log
        activityLogRepository.save(ActivityLog.builder()
                .action(fullName + " removed from congregation roster")
                .module("Member")
                .performedBy(getCurrentUser())
                .timestamp(LocalDateTime.now())
                .build());
    }
}
