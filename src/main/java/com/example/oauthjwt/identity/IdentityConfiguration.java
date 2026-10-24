package com.example.oauthjwt.identity;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

/**
 * Supplies the sample end-user identity used by interactive authorization-code
 * flows. Production applications should load users from a persistent identity
 * store instead.
 */
@Configuration
public class IdentityConfiguration {

    /**
     * Creates the in-memory demonstration user.
     *
     * @param passwordEncoder encoder used to hash the sample password
     * @return the user-details service used by Spring Security login
     */
    @Bean
    UserDetailsService userDetailsService(PasswordEncoder passwordEncoder) {
        return new InMemoryUserDetailsManager(
                User.withUsername("demo-user")
                        .password(passwordEncoder.encode("changeit"))
                        .roles("USER")
                        .build());
    }
}
