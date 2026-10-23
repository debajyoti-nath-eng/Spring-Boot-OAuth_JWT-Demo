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
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OAuthJwtApiIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

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
    void allowsPublicApiWithoutToken() {
        var response = restTemplate.getForEntity("/api/public", Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
