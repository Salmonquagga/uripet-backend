package com.dbp.uripet.health.controller;

import com.dbp.uripet.health.domain.HealthRecordType;
import com.dbp.uripet.health.dto.HealthRequestDto;
import com.dbp.uripet.health.dto.HealthResponseDto;
import com.dbp.uripet.health.service.HealthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/pets/{pid}/health-records")
@RequiredArgsConstructor
public class HealthController {
    private final HealthService healthService;

    @PostMapping
    @Operation(security = {@SecurityRequirement(name = "bearerAuth")})
    public ResponseEntity<HealthResponseDto> createRecord(
            @PathVariable String pid,
            @Valid @RequestBody HealthRequestDto request
    ) {
        return new ResponseEntity<>(healthService.createRecord(pid, request), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<Page<HealthResponseDto>> getRecords(
            @PathVariable String pid,
            @RequestParam(required = false) HealthRecordType type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size
    ) {
        return ResponseEntity.ok(healthService.getRecords(pid, type, page, size));
    }

    @PatchMapping("/{recordId}")
    @Operation(security = {@SecurityRequirement(name = "bearerAuth")})
    public ResponseEntity<HealthResponseDto> updateRecord(
            @PathVariable String pid,
            @PathVariable Long recordId,
            @Valid @RequestBody HealthRequestDto request
    ) {
        return ResponseEntity.ok(healthService.updateRecord(pid, recordId, request));
    }

    @DeleteMapping("/{recordId}")
    @Operation(security = {@SecurityRequirement(name = "bearerAuth")})
    public ResponseEntity<Void> deleteRecord(
            @PathVariable String pid,
            @PathVariable Long recordId
    ) {
        healthService.deleteRecord(pid, recordId);
        return ResponseEntity.noContent().build();
    }
}