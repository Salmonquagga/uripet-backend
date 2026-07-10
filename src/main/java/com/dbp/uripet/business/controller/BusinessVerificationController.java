package com.dbp.uripet.business.controller;

import com.dbp.uripet.business.dto.BusinessVerificationRequestDto;
import com.dbp.uripet.business.dto.BusinessVerificationResponseDto;
import com.dbp.uripet.business.dto.BusinessVerificationReviewDto;
import com.dbp.uripet.business.service.BusinessVerificationService;
import com.dbp.uripet.user.domain.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/business-verifications")
@RequiredArgsConstructor
public class BusinessVerificationController {

    private final BusinessVerificationService businessVerificationService;

    @PostMapping
    public ResponseEntity<BusinessVerificationResponseDto> createRequest(
            @Valid @RequestBody BusinessVerificationRequestDto request,
            @AuthenticationPrincipal User currentUser
    ) {
        return ResponseEntity.ok(businessVerificationService.createRequest(request, currentUser));
    }

    @GetMapping("/me")
    public ResponseEntity<List<BusinessVerificationResponseDto>> getMyRequests(
            @AuthenticationPrincipal User currentUser
    ) {
        return ResponseEntity.ok(businessVerificationService.getMyRequests(currentUser));
    }

    @GetMapping("/{requestUid}")
    public ResponseEntity<BusinessVerificationResponseDto> getRequest(
            @PathVariable String requestUid,
            @AuthenticationPrincipal User currentUser
    ) {
        return ResponseEntity.ok(businessVerificationService.getRequest(requestUid, currentUser));
    }

    @PatchMapping("/{requestUid}/cancel")
    public ResponseEntity<BusinessVerificationResponseDto> cancelMyRequest(
            @PathVariable String requestUid,
            @AuthenticationPrincipal User currentUser
    ) {
        return ResponseEntity.ok(businessVerificationService.cancelMyRequest(requestUid, currentUser));
    }

    @GetMapping("/admin")
    public ResponseEntity<List<BusinessVerificationResponseDto>> getAdminRequests(
            @RequestParam(required = false) String status,
            @AuthenticationPrincipal User currentUser
    ) {
        return ResponseEntity.ok(businessVerificationService.getAdminRequests(status, currentUser));
    }

    @PatchMapping("/admin/{requestUid}/review")
    public ResponseEntity<BusinessVerificationResponseDto> reviewRequest(
            @PathVariable String requestUid,
            @Valid @RequestBody BusinessVerificationReviewDto request,
            @AuthenticationPrincipal User currentUser
    ) {
        return ResponseEntity.ok(businessVerificationService.reviewRequest(requestUid, request, currentUser));
    }
}
