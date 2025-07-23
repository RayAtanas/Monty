package com.monty.backend;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories
@ComponentScan(basePackages = "com.monty.backend")
@OpenAPIDefinition(
		info = @Info(title = "Monty Mobile API", version = "v1"),
		security = @SecurityRequirement(name = "bearerAuth")      // apply globally
)
@SecurityScheme(
		name             = "bearerAuth",
		type             = SecuritySchemeType.HTTP,
		scheme           = "bearer",
		bearerFormat     = "JWT",
		description      = "Enter your JWT token as: Bearer <token>"
)
public class BackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(BackendApplication.class, args);
	}

}
