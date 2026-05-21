package com.cloudconfig.smart.pay.hub.sph.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConfigUpdateRequest {
    
    @NotBlank(message = "Service name cannot be blank")
    private String serviceName;
    
    @NotBlank(message = "Environment cannot be blank")
    private String environment;
    
    @NotBlank(message = "Config key cannot be blank")
    private String configKey;
    
    @NotBlank(message = "Config value cannot be blank")
    private String configValue;
    
    private String description;
    private Boolean isEncrypted = false;
    private String changeReason;
}