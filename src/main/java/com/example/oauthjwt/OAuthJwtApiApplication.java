package com.example.oauthjwt;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Starts the combined OAuth 2.0 authorization server, OpenID Connect identity
 * provider, and JWT-protected resource server.
 */
@SpringBootApplication
public class OAuthJwtApiApplication {

    /**
     * Starts the Spring Boot application.
     *
     * @param args command-line arguments passed to Spring Boot
     */
    public static void main(String[] args) {
        SpringApplication.run(OAuthJwtApiApplication.class, args);
    }
}
