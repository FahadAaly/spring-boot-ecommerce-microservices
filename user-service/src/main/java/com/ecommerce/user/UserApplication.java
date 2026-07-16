package com.ecommerce.user;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.beans.factory.annotation.Value;
@SpringBootApplication
public class UserApplication {

	public static void main(String[] args) {
		SpringApplication.run(UserApplication.class, args);


	}

	@Value("${spring.mongodb.uri}")
	private String mongoUri;

	@PostConstruct
	public void logUri() {
		System.out.println("Mongo URI: " + mongoUri);
	}

}
