package com.cloudconfig.smart.pay.hub.sph.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "configuration_audits", indexes = {
    @Index(name = "idx_config_id", columnList = "config_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConfigurationAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String configId;

    @Column(nullable = false, length = 50)
    private String action; // CREATE, UPDATE, DELETE

    @Column(columnDefinition = "TEXT")
    private String previousValue;

    @Column(columnDefinition = "TEXT")
    private String newValue;

    @Column(nullable = false, length = 100)
    private String modifiedBy;

    @CreationTimestamp
    private LocalDateTime modifiedAt;

    @Column(columnDefinition = "TEXT")
    private String changeReason;
}