package com.merchant.smart.pay.hub.sph.repository;

import com.merchant.smart.pay.hub.sph.entity.MerchantEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface MerchantRepository extends JpaRepository<MerchantEntity, String> {

    Optional<MerchantEntity> findByMerchantCode(String merchantCode);

    Optional<MerchantEntity> findByBusinessEmail(String businessEmail);

    Page<MerchantEntity> findByStatusAndIsActiveTrue(MerchantEntity.MerchantStatus status, Pageable pageable);

    Page<MerchantEntity> findByIsActiveTrue(Pageable pageable);

    List<MerchantEntity> findByBusinessCityAndIsActiveTrue(String city);

    @Query("SELECT m FROM MerchantEntity m WHERE m.isActive = true AND m.status = 'ACTIVE' ORDER BY m.createdAt DESC")
    Page<MerchantEntity> findAllActiveMerchants(Pageable pageable);

    @Query("SELECT COUNT(m) FROM MerchantEntity m WHERE m.status = :status")
    long countByStatus(@Param("status") MerchantEntity.MerchantStatus status);
}