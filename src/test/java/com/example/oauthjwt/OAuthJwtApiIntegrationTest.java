package com.example.oauthjwt;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OAuthJwtApiIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JwtDecoder jwtDecoder;

    @Test
    void issuesJwtAndAcceptsItForProtectedApi() {
        HttpHeaders tokenHeaders = new HttpHeaders();
        tokenHeaders.setBasicAuth("demo-client", "demo-secret");
        tokenHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("scope", "api.read");

        Map<?, ?> tokenResponse = restTemplate.postForObject(
                "/oauth2/token", new HttpEntity<>(form, tokenHeaders), Map.class);
        assertThat(tokenResponse).isNotNull();
        assertThat(tokenResponse.get("access_token")).isNotNull();
        String token = (String) tokenResponse.get("access_token");
        Jwt decodedToken = jwtDecoder.decode(token);
        assertThat(decodedToken.getIssuer()).hasToString("http://localhost:9000");
        assertThat(decodedToken.getSubject()).isEqualTo("demo-client");
        assertThat(decodedToken.getClaimAsStringList("scope")).contains("api.read");
        assertThat(decodedToken.getExpiresAt()).isNotNull();

        HttpHeaders apiHeaders = new HttpHeaders();
        apiHeaders.setBearerAuth(token);
        var apiResponse = restTemplate.exchange(
                "/api/profile", HttpMethod.GET, new HttpEntity<>(apiHeaders), Map.class);

        assertThat(apiResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<?, ?> profile = apiResponse.getBody();
        assertThat(profile.get("subject")).isEqualTo("demo-client");
    }

    @Test
    void rejectsInvalidBearerToken() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth("not-a-valid-jwt");

        var response = restTemplate.exchange(
                "/api/profile", HttpMethod.GET, new HttpEntity<>(headers), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void rejectsTokenWithoutRequiredScope() {
        HttpHeaders tokenHeaders = new HttpHeaders();
        tokenHeaders.setBasicAuth("demo-client", "demo-secret");
        tokenHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("scope", "api.write");
        Map<?, ?> tokenResponse = restTemplate.postForObject(
                "/oauth2/token", new HttpEntity<>(form, tokenHeaders), Map.class);

        HttpHeaders apiHeaders = new HttpHeaders();
        apiHeaders.setBearerAuth((String) tokenResponse.get("access_token"));
        var response = restTemplate.exchange(
                "/api/profile", HttpMethod.GET, new HttpEntity<>(apiHeaders), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void grantsAdminRouteOnlyWithWriteScope() {
        HttpHeaders tokenHeaders = new HttpHeaders();
        tokenHeaders.setBasicAuth("demo-client", "demo-secret");
        tokenHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("scope", "api.write");
        Map<?, ?> tokenResponse = restTemplate.postForObject(
                "/oauth2/token", new HttpEntity<>(form, tokenHeaders), Map.class);

        HttpHeaders apiHeaders = new HttpHeaders();
        apiHeaders.setBearerAuth((String) tokenResponse.get("access_token"));
        var response = restTemplate.exchange(
                "/api/admin/status", HttpMethod.GET, new HttpEntity<>(apiHeaders), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void allowsPublicApiWithoutToken() {
        var response = restTemplate.getForEntity("/api/public", Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void publishesOpenIdProviderMetadataAndSigningKeys() {
        var metadataResponse = restTemplate.getForEntity(
                "/.well-known/openid-configuration", Map.class);
        var keysResponse = restTemplate.getForEntity("/oauth2/jwks", Map.class);

        assertThat(metadataResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(metadataResponse.getBody()).containsEntry(
                "issuer", "http://localhost:9000");
        assertThat(metadataResponse.getBody()).containsKey("userinfo_endpoint");
        assertThat(keysResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(keysResponse.getBody()).containsKey("keys");
    }
}
