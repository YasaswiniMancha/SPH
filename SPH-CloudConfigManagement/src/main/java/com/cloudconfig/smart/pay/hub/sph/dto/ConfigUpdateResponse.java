package com.cloudconfig.smart.pay.hub.sph.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConfigUpdateResponse {
    private String id;
    private String message;
    private String s3ObjectKey;
    private Long timestamp;
    private String configKey;
}