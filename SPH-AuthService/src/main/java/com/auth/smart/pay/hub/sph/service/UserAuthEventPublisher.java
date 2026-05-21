package com.auth.smart.pay.hub.sph.service;

import com.auth.smart.pay.hub.sph.kafka.UserAuthenticationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserAuthEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private static final String TOPIC = "user-auth-events";

    public void publishUserRegistration(String userId, String username, String email) {
        UserAuthenticationEvent event = new UserAuthenticationEvent(userId, username, email, "REGISTER");
        kafkaTemplate.send(TOPIC, event);
    }

    public void publishUserLogin(String userId, String username) {
        UserAuthenticationEvent event = new UserAuthenticationEvent(userId, username, null, "LOGIN");
        kafkaTemplate.send(TOPIC, event);
    }

    public void publishUserLogout(String username) {
        UserAuthenticationEvent event = new UserAuthenticationEvent(null, username, null, "LOGOUT");
        kafkaTemplate.send(TOPIC, event);
    }
}