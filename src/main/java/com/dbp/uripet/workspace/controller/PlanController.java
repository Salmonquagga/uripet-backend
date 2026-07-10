package com.dbp.uripet.workspace.controller;

import com.dbp.uripet.workspace.domain.enums.PlanType;
import com.dbp.uripet.workspace.dto.PlanLimitsResponseDto;
import com.dbp.uripet.workspace.service.PlanLimitsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/plans")
@RequiredArgsConstructor
public class PlanController {

    private final PlanLimitsService
            planLimitsService;

    @GetMapping
    public ResponseEntity<
            List<PlanLimitsResponseDto>
            >
    getPlans() {
        List<PlanLimitsResponseDto> plans =
                Arrays.stream(
                                PlanType.values()
                        )
                        .map(
                                planLimitsService
                                        ::getEmptyLimits
                        )
                        .toList();

        return ResponseEntity.ok(plans);
    }

    @GetMapping("/{planType}")
    public ResponseEntity<PlanLimitsResponseDto>
    getPlan(
            @PathVariable
            PlanType planType
    ) {
        return ResponseEntity.ok(
                planLimitsService
                        .getEmptyLimits(
                                planType
                        )
        );
    }
}