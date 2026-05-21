package com.cloudconfig.smart.pay.hub.sph.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConfigurationDto {
    private String id;
    private String serviceName;
    private String environment;
    private String configKey;
    private String configValue;
    private String description;
    private Boolean isEncrypted;
    private Boolean isActive;
    private Long version;
}