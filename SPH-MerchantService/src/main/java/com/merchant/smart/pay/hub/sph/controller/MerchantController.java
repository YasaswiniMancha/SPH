package com.merchant.smart.pay.hub.sph.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.common.smart.pay.hub.sph.dto.response.ApiResponse;
import com.merchant.smart.pay.hub.sph.dto.request.MerchantDTO;
import com.merchant.smart.pay.hub.sph.dto.response.MerchantResponseDTO;
import com.merchant.smart.pay.hub.sph.entity.MerchantEntity;
import com.merchant.smart.pay.hub.sph.service.MerchantService;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/v1/merchants")
@CrossOrigin(origins = "*")
public class MerchantController {

    private final MerchantService merchantService;

    public MerchantController(MerchantService merchantService) {
        this.merchantService = merchantService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<MerchantResponseDTO>> createMerchant(
            @Valid @RequestBody MerchantDTO request) {
        log.info("Creating merchant: {}", request.getMerchantCode());
        MerchantResponseDTO result = merchantService.createMerchant(request);
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.success(result, "Merchant created successfully"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<MerchantResponseDTO>> getMerchantById(@PathVariable String id) {
        log.info("Fetching merchant: {}", id);
        MerchantResponseDTO result = merchantService.getMerchantById(id);
        return ResponseEntity.ok(ApiResponse.success(result, "Merchant retrieved successfully"));
    }

    @GetMapping("/code/{merchantCode}")
    public ResponseEntity<ApiResponse<MerchantResponseDTO>> getMerchantByCode(
            @PathVariable String merchantCode) {
        log.info("Fetching merchant by code: {}", merchantCode);
        MerchantResponseDTO result = merchantService.getMerchantByCode(merchantCode);
        return ResponseEntity.ok(ApiResponse.success(result, "Merchant retrieved successfully"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<MerchantResponseDTO>>> getAllMerchants(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("Fetching all merchants - page: {}, size: {}", page, size);
        Page<MerchantResponseDTO> result = merchantService.getAllMerchants(PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(result, "Merchants retrieved successfully"));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<ApiResponse<Page<MerchantResponseDTO>>> getMerchantsByStatus(
            @PathVariable String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("Fetching merchants by status: {}", status);
        MerchantEntity.MerchantStatus merchantStatus = MerchantEntity.MerchantStatus.valueOf(status.toUpperCase());
        Page<MerchantResponseDTO> result = merchantService.getMerchantsByStatus(
            merchantStatus, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(result, "Merchants retrieved successfully"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<MerchantResponseDTO>> updateMerchant(
            @PathVariable String id,
            @Valid @RequestBody MerchantDTO request) {
        log.info("Updating merchant: {}", id);
        MerchantResponseDTO result = merchantService.updateMerchant(id, request);
        return ResponseEntity.ok(ApiResponse.success(result, "Merchant updated successfully"));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<MerchantResponseDTO>> approveMerchant(@PathVariable String id) {
        log.info("Approving merchant: {}", id);
        MerchantResponseDTO result = merchantService.approveMerchant(id);
        return ResponseEntity.ok(ApiResponse.success(result, "Merchant approved successfully"));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<ApiResponse<MerchantResponseDTO>> rejectMerchant(
            @PathVariable String id,
            @RequestParam String reason) {
        log.info("Rejecting merchant: {}", id);
        MerchantResponseDTO result = merchantService.rejectMerchant(id, reason);
        return ResponseEntity.ok(ApiResponse.success(result, "Merchant rejected successfully"));
    }

    @PostMapping("/{id}/suspend")
    public ResponseEntity<ApiResponse<MerchantResponseDTO>> suspendMerchant(
            @PathVariable String id,
            @RequestParam String reason) {
        log.info("Suspending merchant: {}", id);
        MerchantResponseDTO result = merchantService.suspendMerchant(id, reason);
        return ResponseEntity.ok(ApiResponse.success(result, "Merchant suspended successfully"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteMerchant(@PathVariable String id) {
        log.info("Deleting merchant: {}", id);
        merchantService.deleteMerchant(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Merchant deleted successfully"));
    }

    @GetMapping("/health")
    public ResponseEntity<ApiResponse<?>> health() {
        return ResponseEntity.ok(ApiResponse.success(
            "UP",
            "Merchant service is healthy"
        ));
    }
}