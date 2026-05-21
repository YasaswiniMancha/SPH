package com.merchant.smart.pay.hub.sph.service;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.common.smart.pay.hub.sph.exception.BusinessException;
import com.common.smart.pay.hub.sph.exception.ResourceNotFoundException;
import com.merchant.smart.pay.hub.sph.dto.request.MerchantDTO;
import com.merchant.smart.pay.hub.sph.dto.response.MerchantResponseDTO;
import com.merchant.smart.pay.hub.sph.entity.MerchantEntity;
import com.merchant.smart.pay.hub.sph.repository.MerchantRepository;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.micrometer.core.annotation.Timed;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional
public class MerchantService {

    private final MerchantRepository merchantRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    // Add to existing MerchantService.java

    @Autowired
    private DistributedLockService distributedLockService;

    @Autowired
    private MerchantMetricsService metricsService;

    @Autowired
    private MerchantAsyncService asyncService;

    @CacheEvict(value = "merchants", allEntries = true)
    public MerchantResponseDTO approveMerchantWithLock(String id) {
        return distributedLockService.executeWithLock("merchant:" + id, () -> {
            MerchantEntity merchant = merchantRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Merchant not found"));
            merchant.setStatus(MerchantEntity.MerchantStatus.ACTIVE);
            merchantRepository.save(merchant);
            metricsService.recordMerchantApproved();
            return toResponseDTO(merchant);
        });
    }

    public CompletableFuture<Boolean> verifyMerchantAsync(String merchantId) {
        return asyncService.verifyMerchantAsync(merchantId);
    }

    public MerchantService(MerchantRepository merchantRepository, KafkaTemplate<String, String> kafkaTemplate) {
        this.merchantRepository = merchantRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Timed(value = "merchant.create", description = "Create merchant time")
    @CircuitBreaker(name = "merchantServiceCB", fallbackMethod = "createMerchantFallback")
    public MerchantResponseDTO createMerchant(MerchantDTO request) {
        log.info("Creating merchant: {}", request.getMerchantCode());

        // Check if merchant already exists
        if (merchantRepository.findByMerchantCode(request.getMerchantCode()).isPresent()) {
            throw new BusinessException("Merchant with code " + request.getMerchantCode() + " already exists");
        }

        if (merchantRepository.findByBusinessEmail(request.getBusinessEmail()).isPresent()) {
            throw new BusinessException("Merchant with email " + request.getBusinessEmail() + " already exists");
        }

        MerchantEntity merchant = MerchantEntity.builder()
            .merchantCode(request.getMerchantCode())
            .businessName(request.getBusinessName())
            .businessDescription(request.getBusinessDescription())
            .businessEmail(request.getBusinessEmail())
            .businessPhone(request.getBusinessPhone())
            .businessAddress(request.getBusinessAddress())
            .businessCity(request.getBusinessCity())
            .businessState(request.getBusinessState())
            .businessZipCode(request.getBusinessZipCode())
            .contactPersonName(request.getContactPersonName())
            .contactPersonEmail(request.getContactPersonEmail())
            .contactPersonPhone(request.getContactPersonPhone())
            .taxId(request.getTaxId())
            .registrationNumber(request.getRegistrationNumber())
            .bankAccountNumber(request.getBankAccountNumber())
            .bankName(request.getBankName())
            .status(MerchantEntity.MerchantStatus.PENDING)
            .isActive(true)
            .build();

        MerchantEntity saved = merchantRepository.save(merchant);
        log.info("Merchant created successfully: {}", saved.getId());

        publishEvent("MERCHANT_CREATED", saved);

        return toResponseDTO(saved);
    }

    @Cacheable(value = "merchants", key = "#id")
    @Timed(value = "merchant.get", description = "Get merchant time")
    public MerchantResponseDTO getMerchantById(String id) {
        log.info("Fetching merchant: {}", id);
        MerchantEntity merchant = merchantRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Merchant not found with id: " + id));
        return toResponseDTO(merchant);
    }

    @Cacheable(value = "merchants", key = "#merchantCode")
    public MerchantResponseDTO getMerchantByCode(String merchantCode) {
        log.info("Fetching merchant by code: {}", merchantCode);
        MerchantEntity merchant = merchantRepository.findByMerchantCode(merchantCode)
            .orElseThrow(() -> new ResourceNotFoundException("Merchant not found with code: " + merchantCode));
        return toResponseDTO(merchant);
    }

    @Timed(value = "merchant.list", description = "List merchants time")
    public Page<MerchantResponseDTO> getAllMerchants(Pageable pageable) {
        log.info("Fetching all active merchants");
        return merchantRepository.findAllActiveMerchants(pageable)
            .map(this::toResponseDTO);
    }

    @Timed(value = "merchant.list.status", description = "List merchants by status time")
    public Page<MerchantResponseDTO> getMerchantsByStatus(MerchantEntity.MerchantStatus status, Pageable pageable) {
        log.info("Fetching merchants with status: {}", status);
        return merchantRepository.findByStatusAndIsActiveTrue(status, pageable)
            .map(this::toResponseDTO);
    }

    @CacheEvict(value = "merchants", allEntries = true)
    @Timed(value = "merchant.update", description = "Update merchant time")
    public MerchantResponseDTO updateMerchant(String id, MerchantDTO request) {
        log.info("Updating merchant: {}", id);

        MerchantEntity merchant = merchantRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Merchant not found with id: " + id));

        merchant.setBusinessName(request.getBusinessName());
        merchant.setBusinessDescription(request.getBusinessDescription());
        merchant.setBusinessPhone(request.getBusinessPhone());
        merchant.setBusinessAddress(request.getBusinessAddress());
        merchant.setBusinessCity(request.getBusinessCity());
        merchant.setBusinessState(request.getBusinessState());
        merchant.setBusinessZipCode(request.getBusinessZipCode());
        merchant.setContactPersonName(request.getContactPersonName());
        merchant.setContactPersonEmail(request.getContactPersonEmail());
        merchant.setContactPersonPhone(request.getContactPersonPhone());
        merchant.setBankAccountNumber(request.getBankAccountNumber());
        merchant.setBankName(request.getBankName());

        MerchantEntity updated = merchantRepository.save(merchant);
        log.info("Merchant updated successfully: {}", id);

        publishEvent("MERCHANT_UPDATED", updated);

        return toResponseDTO(updated);
    }

    @CacheEvict(value = "merchants", allEntries = true)
    @Timed(value = "merchant.approve", description = "Approve merchant time")
    public MerchantResponseDTO approveMerchant(String id) {
        log.info("Approving merchant: {}", id);

        MerchantEntity merchant = merchantRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Merchant not found with id: " + id));

        merchant.setStatus(MerchantEntity.MerchantStatus.ACTIVE);
        merchant.setVerifiedAt(LocalDateTime.now());

        MerchantEntity approved = merchantRepository.save(merchant);
        log.info("Merchant approved: {}", id);

        publishEvent("MERCHANT_APPROVED", approved);

        return toResponseDTO(approved);
    }

    @CacheEvict(value = "merchants", allEntries = true)
    @Timed(value = "merchant.reject", description = "Reject merchant time")
    public MerchantResponseDTO rejectMerchant(String id, String reason) {
        log.info("Rejecting merchant: {}", id);

        MerchantEntity merchant = merchantRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Merchant not found with id: " + id));

        merchant.setStatus(MerchantEntity.MerchantStatus.REJECTED);
        merchant.setRejectedReason(reason);

        MerchantEntity rejected = merchantRepository.save(merchant);
        log.info("Merchant rejected: {}", id);

        publishEvent("MERCHANT_REJECTED", rejected);

        return toResponseDTO(rejected);
    }

    @CacheEvict(value = "merchants", allEntries = true)
    @Timed(value = "merchant.suspend", description = "Suspend merchant time")
    public MerchantResponseDTO suspendMerchant(String id, String reason) {
        log.info("Suspending merchant: {}", id);

        MerchantEntity merchant = merchantRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Merchant not found with id: " + id));

        merchant.setStatus(MerchantEntity.MerchantStatus.SUSPENDED);
        merchant.setRejectedReason(reason);

        MerchantEntity suspended = merchantRepository.save(merchant);
        log.info("Merchant suspended: {}", id);

        publishEvent("MERCHANT_SUSPENDED", suspended);

        return toResponseDTO(suspended);
    }

    @CacheEvict(value = "merchants", allEntries = true)
    public void deleteMerchant(String id) {
        log.info("Deleting merchant: {}", id);

        MerchantEntity merchant = merchantRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Merchant not found with id: " + id));

        merchant.setIsActive(false);
        merchantRepository.save(merchant);

        publishEvent("MERCHANT_DELETED", merchant);
        log.info("Merchant deleted: {}", id);
    }

    public MerchantResponseDTO createMerchantFallback(MerchantDTO request, Exception ex) {
        log.error("Merchant creation fallback triggered: {}", ex.getMessage());
        throw new BusinessException("Merchant service temporarily unavailable. Please try again later.");
    }

    private void publishEvent(String eventType, MerchantEntity merchant) {
        try {
            String message = String.format(
                "merchantId=%s,merchantCode=%s,eventType=%s,timestamp=%d",
                merchant.getId(),
                merchant.getMerchantCode(),
                eventType,
                System.currentTimeMillis()
            );
            kafkaTemplate.send("merchant-events", message);
            log.debug("Merchant event published: {}", eventType);
        } catch (Exception e) {
            log.warn("Failed to publish merchant event", e);
        }
    }


    private MerchantResponseDTO toResponseDTO(MerchantEntity entity) {
        return MerchantResponseDTO.builder()
            .id(entity.getId())
            .merchantCode(entity.getMerchantCode())
            .businessName(entity.getBusinessName())
            .businessEmail(entity.getBusinessEmail())
            .businessPhone(entity.getBusinessPhone())
            .status(entity.getStatus().toString())
            .dailyTransactionLimit(entity.getDailyTransactionLimit())
            .monthlyTransactionLimit(entity.getMonthlyTransactionLimit())
            .isActive(entity.getIsActive())
            .createdAt(entity.getCreatedAt())
            .verifiedAt(entity.getVerifiedAt())
            .build();
    }
}