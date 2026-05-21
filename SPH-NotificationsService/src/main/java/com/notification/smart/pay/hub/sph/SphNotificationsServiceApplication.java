package com.notification.smart.pay.hub.sph;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class SphNotificationsServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(SphNotificationsServiceApplication.class, args);
	}

}
