package com.example.oauthjwt.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configures bearer-token authentication and scope-based authorization for API
 * resources. JWT decoding and issuer validation are provided by the shared
 * authorization-server configuration in this single-process demonstration.
 */
@Configuration
public class ResourceServerConfiguration {

    /**
     * Allows public API access, requires {@code api.read} for ordinary API
     * routes, and requires {@code api.write} for administrative routes.
     *
     * @param http the HTTP security builder
     * @return the resource-server filter chain
     * @throws Exception if the filter chain cannot be configured
     */
    @Bean
    @Order(2)
    SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/api/public").permitAll()
                        .requestMatchers("/api/admin/**").hasAuthority("SCOPE_api.write")
                        .requestMatchers("/api/**").hasAuthority("SCOPE_api.read")
                        .anyRequest().authenticated())
                .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"))
                .oauth2ResourceServer(resourceServer -> resourceServer
                        .jwt(Customizer.withDefaults())
                        .authenticationEntryPoint(new BearerTokenAuthenticationEntryPoint()))
                .formLogin(Customizer.withDefaults());
        return http.build();
    }
}
