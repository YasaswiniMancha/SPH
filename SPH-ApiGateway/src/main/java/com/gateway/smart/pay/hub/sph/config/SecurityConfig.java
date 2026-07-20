package com.gateway.smart.pay.hub.sph.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig  {

    @Bean
    @ConditionalOnMissingBean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
        grantedAuthoritiesConverter.setAuthorityPrefix("SCOPE_");
        grantedAuthoritiesConverter.setAuthoritiesClaimName("scope");

        JwtAuthenticationConverter authenticationConverter = new JwtAuthenticationConverter();
        authenticationConverter.setJwtGrantedAuthoritiesConverter(grantedAuthoritiesConverter);
        return authenticationConverter;
    }

    @Bean
    @ConditionalOnProperty(prefix = "security.jwt", name = "enabled", havingValue = "true")
    SecurityFilterChain jwtEnabledFilterChain(HttpSecurity http, JwtAuthenticationConverter jwtAuthenticationConverter)
        throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                .requestMatchers("/api/v1/auth/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/wallet/**").hasAuthority("SCOPE_wallet.read")
                .requestMatchers(HttpMethod.POST, "/api/v1/wallet/**").hasAuthority("SCOPE_wallet.write")
                .requestMatchers(HttpMethod.PUT, "/api/v1/wallet/**").hasAuthority("SCOPE_wallet.write")
                .requestMatchers(HttpMethod.PATCH, "/api/v1/wallet/**").hasAuthority("SCOPE_wallet.write")
                .requestMatchers(HttpMethod.DELETE, "/api/v1/wallet/**").hasAuthority("SCOPE_wallet.write")
                .requestMatchers(HttpMethod.GET, "/api/v1/wallet-transactions/**").hasAuthority("SCOPE_txn.read")
                .requestMatchers(HttpMethod.POST, "/api/v1/wallet-transactions/**").hasAuthority("SCOPE_txn.write")
                .requestMatchers(HttpMethod.PUT, "/api/v1/wallet-transactions/**").hasAuthority("SCOPE_txn.write")
                .requestMatchers(HttpMethod.PATCH, "/api/v1/wallet-transactions/**").hasAuthority("SCOPE_txn.write")
                .requestMatchers(HttpMethod.DELETE, "/api/v1/wallet-transactions/**").hasAuthority("SCOPE_txn.write")
                .anyRequest().denyAll()
            )
            .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)));
        return http.build();
    }

    @Bean
    @ConditionalOnProperty(prefix = "security.jwt", name = "enabled", havingValue = "false", matchIfMissing = true)
    SecurityFilterChain jwtDisabledFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());

        return http.build();
    }
}
