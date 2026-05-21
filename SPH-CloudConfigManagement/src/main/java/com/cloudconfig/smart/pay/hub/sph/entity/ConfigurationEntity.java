package com.cloudconfig.smart.pay.hub.sph.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "configurations", indexes = {
    @Index(name = "idx_service_env", columnList = "service_name, environment"),
    @Index(name = "idx_config_key", columnList = "config_key")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConfigurationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, length = 100)
    private String serviceName;

    @Column(nullable = false, length = 50)
    private String environment; // dev, staging, prod

    @Column(nullable = false, length = 255)
    private String configKey;

    @Column(columnDefinition = "TEXT")
    private String configValue;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "s3_object_key", length = 500)
    private String s3ObjectKey;

    @Version
    private Long version;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "is_encrypted")
    private Boolean isEncrypted = false;

    @Column(name = "is_active")
    private Boolean isActive = true;
}