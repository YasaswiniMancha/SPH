package com.cloudconfig.smart.pay.hub.sph.repository;

import com.cloudconfig.smart.pay.hub.sph.entity.ConfigurationEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface ConfigurationRepository extends JpaRepository<ConfigurationEntity, String> {
    
    Optional<ConfigurationEntity> findByServiceNameAndEnvironmentAndConfigKey(
        String serviceName, String environment, String configKey);
    
    Page<ConfigurationEntity> findByServiceNameAndEnvironment(
        String serviceName, String environment, Pageable pageable);
    
    List<ConfigurationEntity> findByServiceNameAndEnvironmentAndIsActiveTrue(
        String serviceName, String environment);
    
    Page<ConfigurationEntity> findByIsActiveTrue(Pageable pageable);
}