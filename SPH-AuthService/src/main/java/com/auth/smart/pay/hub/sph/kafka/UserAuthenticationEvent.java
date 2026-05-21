package com.auth.smart.pay.hub.sph.kafka;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserAuthenticationEvent {
    private String userId;
    private String username;
    private String email;
    private String eventType;
}