package com.cloudconfig.smart.pay.hub.sph;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.config.server.EnableConfigServer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@SpringBootApplication
@EnableConfigServer
@EnableCaching
@EnableScheduling
public class SphCloudConfigManagementApplication {

	public static void main(String[] args) {
		SpringApplication.run(SphCloudConfigManagementApplication.class, args);
	}

	@Bean(name = "configExecutor")
	public Executor configExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(5);
		executor.setMaxPoolSize(10);
		executor.setQueueCapacity(100);
		executor.setThreadNamePrefix("config-async-");
		executor.initialize();
		return executor;
	}
}