package com.stayease.notification_service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@Slf4j
public class NotificationServiceApplication {

	public static void main(String[] args) {
		log.info("Starting Notification  Service Application");
		SpringApplication.run(NotificationServiceApplication.class, args);
		log.info("Notification Service Application Started Successfully on port 8086");
		log.info("API Documentation: http://localhost:8087/swagger-ui.html");
	}

}
