package com.wallet.smart.pay.hub.sph;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SphWalletServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(SphWalletServiceApplication.class, args);
	}

}
