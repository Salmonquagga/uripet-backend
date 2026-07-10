package com.dbp.uripet.business.service;

import com.dbp.uripet.business.domain.BusinessVerificationRequest;
import com.dbp.uripet.business.domain.enums.BusinessType;
import com.dbp.uripet.business.domain.enums.BusinessVerificationStatus;
import com.dbp.uripet.business.dto.BusinessVerificationRequestDto;
import com.dbp.uripet.business.dto.BusinessVerificationResponseDto;
import com.dbp.uripet.business.dto.BusinessVerificationReviewDto;
import com.dbp.uripet.business.repository.BusinessVerificationRequestRepository;
import com.dbp.uripet.config.error.ConflictException;
import com.dbp.uripet.config.error.ForbiddenException;
import com.dbp.uripet.config.error.InvalidOperationException;
import com.dbp.uripet.config.error.ResourceNotFoundException;
import com.dbp.uripet.config.error.ValidationException;
import com.dbp.uripet.events.BusinessVerificationApprovedEvent;
import com.dbp.uripet.events.BusinessVerificationRejectedEvent;
import com.dbp.uripet.events.BusinessVerificationRequestedEvent;
import com.dbp.uripet.user.domain.User;
import com.dbp.uripet.user.domain.enums.UserRole;
import com.dbp.uripet.workspace.domain.Workspace;
import com.dbp.uripet.workspace.domain.WorkspaceMember;
import com.dbp.uripet.workspace.domain.enums.PlanType;
import com.dbp.uripet.workspace.domain.enums.WorkspaceRole;
import com.dbp.uripet.workspace.domain.enums.WorkspaceStatus;
import com.dbp.uripet.workspace.repository.WorkspaceMemberRepository;
import com.dbp.uripet.workspace.repository.WorkspaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.ZonedDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BusinessVerificationService {

    private final BusinessVerificationRequestRepository businessVerificationRequestRepository;
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public BusinessVerificationResponseDto createRequest(
            BusinessVerificationRequestDto request,
            User currentUser
    ) {
        BusinessType businessType = parseBusinessType(request.getBusinessType());
        PlanType planType = toPlanType(businessType);

        if (workspaceRepository.existsByOwnerAndPlanType(currentUser, planType)) {
            throw new ConflictException("You already have an active workspace for this business type");
        }

        boolean hasPendingRequest = businessVerificationRequestRepository.existsByRequesterAndStatusIn(
                currentUser,
                List.of(BusinessVerificationStatus.PENDING)
        );

        if (hasPendingRequest) {
            throw new ConflictException("You already have a pending business verification request");
        }

        BusinessVerificationRequest verificationRequest = BusinessVerificationRequest.builder()
                .requester(currentUser)
                .businessName(request.getBusinessName().trim())
                .businessType(businessType)
                .ruc(clean(request.getRuc()))
                .contactEmail(resolveContactEmail(request.getContactEmail(), currentUser))
                .phone(clean(request.getPhone()))
                .address(clean(request.getAddress()))
                .documentUrl(clean(request.getDocumentUrl()))
                .description(clean(request.getDescription()))
                .status(BusinessVerificationStatus.PENDING)
                .build();

        businessVerificationRequestRepository.save(verificationRequest);

        eventPublisher.publishEvent(new BusinessVerificationRequestedEvent(
                verificationRequest.getUid(),
                currentUser.getEmail(),
                currentUser.getName(),
                verificationRequest.getBusinessName(),
                verificationRequest.getBusinessType().name()
        ));

        return toResponse(verificationRequest);
    }

    @Transactional(readOnly = true)
    public List<BusinessVerificationResponseDto> getMyRequests(User currentUser) {
        return businessVerificationRequestRepository.findByRequesterOrderByCreatedAtDesc(currentUser)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public BusinessVerificationResponseDto getRequest(String requestUid, User currentUser) {
        BusinessVerificationRequest request = getByUid(requestUid);

        boolean isOwner = request.getRequester().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser.getRole() == UserRole.ADMIN;

        if (!isOwner && !isAdmin) {
            throw new ForbiddenException("You are not allowed to view this verification request");
        }

        return toResponse(request);
    }

    @Transactional(readOnly = true)
    public List<BusinessVerificationResponseDto> getAdminRequests(String status, User currentUser) {
        checkAdmin(currentUser);

        if (StringUtils.hasText(status)) {
            BusinessVerificationStatus parsedStatus = parseStatus(status);
            return businessVerificationRequestRepository.findByStatusOrderByCreatedAtDesc(parsedStatus)
                    .stream()
                    .map(this::toResponse)
                    .toList();
        }

        return businessVerificationRequestRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public BusinessVerificationResponseDto reviewRequest(
            String requestUid,
            BusinessVerificationReviewDto reviewDto,
            User currentUser
    ) {
        checkAdmin(currentUser);

        BusinessVerificationRequest request = getByUid(requestUid);

        if (request.getStatus() != BusinessVerificationStatus.PENDING) {
            throw new InvalidOperationException("Only pending requests can be reviewed");
        }

        BusinessVerificationStatus newStatus = parseStatus(reviewDto.getStatus());

        if (newStatus != BusinessVerificationStatus.APPROVED && newStatus != BusinessVerificationStatus.REJECTED) {
            throw new ValidationException("Review status must be APPROVED or REJECTED");
        }

        request.setReviewer(currentUser);
        request.setStatus(newStatus);
        request.setReviewComment(clean(reviewDto.getReviewComment()));
        request.setReviewedAt(ZonedDateTime.now());

        if (newStatus == BusinessVerificationStatus.APPROVED) {
            Workspace workspace = createBusinessWorkspace(request);
            request.setWorkspace(workspace);

            eventPublisher.publishEvent(new BusinessVerificationApprovedEvent(
                    request.getRequester().getEmail(),
                    request.getRequester().getName(),
                    request.getBusinessName(),
                    request.getBusinessType().name(),
                    workspace.getUid()
            ));
        } else {
            eventPublisher.publishEvent(new BusinessVerificationRejectedEvent(
                    request.getRequester().getEmail(),
                    request.getRequester().getName(),
                    request.getBusinessName(),
                    request.getBusinessType().name(),
                    request.getReviewComment()
            ));
        }

        businessVerificationRequestRepository.save(request);
        return toResponse(request);
    }

    @Transactional
    public BusinessVerificationResponseDto cancelMyRequest(String requestUid, User currentUser) {
        BusinessVerificationRequest request = getByUid(requestUid);

        if (!request.getRequester().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("You are not allowed to cancel this verification request");
        }

        if (request.getStatus() != BusinessVerificationStatus.PENDING) {
            throw new InvalidOperationException("Only pending requests can be cancelled");
        }

        request.setStatus(BusinessVerificationStatus.CANCELLED);
        businessVerificationRequestRepository.save(request);

        return toResponse(request);
    }

    private Workspace createBusinessWorkspace(BusinessVerificationRequest request) {
        User owner = request.getRequester();
        PlanType planType = toPlanType(request.getBusinessType());

        if (workspaceRepository.existsByOwnerAndPlanType(owner, planType)) {
            throw new ConflictException("This user already has a workspace for this business type");
        }

        Workspace workspace = Workspace.builder()
                .name(request.getBusinessName())
                .owner(owner)
                .planType(planType)
                .status(WorkspaceStatus.ACTIVE)
                .build();
        workspaceRepository.save(workspace);

        WorkspaceMember member = WorkspaceMember.builder()
                .workspace(workspace)
                .user(owner)
                .role(WorkspaceRole.OWNER)
                .build();
        workspaceMemberRepository.save(member);

        return workspace;
    }

    private BusinessVerificationRequest getByUid(String requestUid) {
        return businessVerificationRequestRepository.findByUid(requestUid)
                .orElseThrow(() -> new ResourceNotFoundException("Business verification request not found"));
    }

    private void checkAdmin(User currentUser) {
        if (currentUser.getRole() != UserRole.ADMIN) {
            throw new ForbiddenException("Admin role required");
        }
    }

    private BusinessType parseBusinessType(String value) {
        try {
            return BusinessType.valueOf(value.trim().toUpperCase());
        } catch (Exception ex) {
            throw new ValidationException("Invalid business type: " + value);
        }
    }

    private BusinessVerificationStatus parseStatus(String value) {
        try {
            return BusinessVerificationStatus.valueOf(value.trim().toUpperCase());
        } catch (Exception ex) {
            throw new ValidationException("Invalid verification status: " + value);
        }
    }

    private PlanType toPlanType(BusinessType businessType) {
        return switch (businessType) {
            case VETERINARY -> PlanType.VETERINARY;
            case SHELTER -> PlanType.SHELTER;
        };
    }

    private String resolveContactEmail(String contactEmail, User currentUser) {
        return StringUtils.hasText(contactEmail) ? contactEmail.trim() : currentUser.getEmail();
    }

    private String clean(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private BusinessVerificationResponseDto toResponse(BusinessVerificationRequest request) {
        User requester = request.getRequester();
        User reviewer = request.getReviewer();
        Workspace workspace = request.getWorkspace();

        return BusinessVerificationResponseDto.builder()
                .uid(request.getUid())
                .requesterUid(requester.getUid())
                .requesterName(requester.getName())
                .requesterEmail(requester.getEmail())
                .reviewerUid(reviewer != null ? reviewer.getUid() : null)
                .reviewerName(reviewer != null ? reviewer.getName() : null)
                .reviewerEmail(reviewer != null ? reviewer.getEmail() : null)
                .workspaceUid(workspace != null ? workspace.getUid() : null)
                .workspaceName(workspace != null ? workspace.getName() : null)
                .businessName(request.getBusinessName())
                .businessType(request.getBusinessType().name())
                .ruc(request.getRuc())
                .contactEmail(request.getContactEmail())
                .phone(request.getPhone())
                .address(request.getAddress())
                .documentUrl(request.getDocumentUrl())
                .description(request.getDescription())
                .status(request.getStatus().name())
                .reviewComment(request.getReviewComment())
                .createdAt(request.getCreatedAt())
                .reviewedAt(request.getReviewedAt())
                .build();
    }
}
