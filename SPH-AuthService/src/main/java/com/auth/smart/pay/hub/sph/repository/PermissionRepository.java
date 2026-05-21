package com.auth.smart.pay.hub.sph.repository;

import com.auth.smart.pay.hub.sph.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, String> {
    
    Optional<Permission> findByCode(String code);
    
    boolean existsByCode(String code);
}