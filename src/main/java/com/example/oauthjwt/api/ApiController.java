package com.example.oauthjwt.api;

import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Example resource-server API endpoints. Authentication and scope enforcement
 * are configured in {@code ResourceServerConfiguration}.
 */
@RestController
public class ApiController {

    /**
     * Returns a message that is accessible without an access token.
     *
     * @return a public response
     */
    @GetMapping("/api/public")
    public Map<String, String> publicEndpoint() {
        return Map.of("message", "This endpoint is public.");
    }

    /**
     * Returns information about the authenticated token subject.
     *
     * @param authentication authenticated principal populated from the validated JWT
     * @return the subject and a protected-resource message
     */
    @GetMapping("/api/profile")
    public Map<String, String> profile(Authentication authentication) {
        return Map.of(
                "subject", authentication.getName(),
                "message", "You accessed a JWT-protected endpoint.");
    }

    /**
     * Example administrative resource protected by the {@code api.write} scope.
     *
     * @return an administrative status response
     */
    @GetMapping("/api/admin/status")
    public Map<String, String> adminStatus() {
        return Map.of("status", "Administrative access granted.");
    }
}
