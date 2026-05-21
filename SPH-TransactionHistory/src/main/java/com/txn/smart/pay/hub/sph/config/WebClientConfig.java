package com.txn.smart.pay.hub.sph.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.util.concurrent.TimeUnit;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient walletServiceWebClient() {
        ConnectionProvider connProvider = ConnectionProvider.builder("wallet-service-pool")
            .maxConnections(200)
            .maxIdleTime(java.time.Duration.ofMinutes(5))
            .build();

        HttpClient httpClient = HttpClient.create(connProvider)
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
            .option(ChannelOption.SO_KEEPALIVE, true)
            .responseTimeout(java.time.Duration.ofSeconds(10))
            .doOnConnected(connection ->
                connection.addHandlerLast(new ReadTimeoutHandler(10, TimeUnit.SECONDS))
                    .addHandlerLast(new WriteTimeoutHandler(10, TimeUnit.SECONDS))
            );

        return WebClient.builder()
            .baseUrl("${wallet-service.url:http://localhost:8082}")
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            .build();
    }

    @Bean
    public WebClient merchantServiceWebClient() {
        ConnectionProvider connProvider = ConnectionProvider.builder("merchant-service-pool")
            .maxConnections(100)
            .maxIdleTime(java.time.Duration.ofMinutes(5))
            .build();

        HttpClient httpClient = HttpClient.create(connProvider)
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
            .responseTimeout(java.time.Duration.ofSeconds(5));

        return WebClient.builder()
            .baseUrl("${merchant-service.url:http://localhost:8083}")
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            .build();
    }
}