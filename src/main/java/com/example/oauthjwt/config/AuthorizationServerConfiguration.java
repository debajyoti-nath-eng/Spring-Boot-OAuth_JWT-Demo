package com.example.oauthjwt.config;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.util.UUID;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;

/**
 * Configures OAuth 2.0 and OpenID Connect protocol endpoints, the demo client,
 * RSA JWT signing and verification, and the configured issuer.
 */
@Configuration
public class AuthorizationServerConfiguration {

    /**
     * Secures protocol endpoints provided by Spring Authorization Server and
     * enables its OpenID Connect endpoints.
     *
     * @param http the HTTP security builder
     * @return the authorization-server filter chain
     * @throws Exception if the filter chain cannot be configured
     */
    @Bean
    @Order(1)
    SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http) throws Exception {
        OAuth2AuthorizationServerConfigurer authorizationServerConfigurer =
                OAuth2AuthorizationServerConfigurer.authorizationServer();
        http.securityMatcher(authorizationServerConfigurer.getEndpointsMatcher())
                .with(authorizationServerConfigurer,
                        authorizationServer -> authorizationServer.oidc(Customizer.withDefaults()))
                .authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated())
                .exceptionHandling(exceptions -> exceptions
                        .defaultAuthenticationEntryPointFor(
                                new LoginUrlAuthenticationEntryPoint("/login"),
                                new MediaTypeRequestMatcher(MediaType.TEXT_HTML)))
                .oauth2ResourceServer(resourceServer -> resourceServer.jwt(Customizer.withDefaults()));
        return http.build();
    }

    /**
     * Registers the in-memory sample OAuth client and its supported grants.
     *
     * @param passwordEncoder encoder used to store the client secret
     * @return the registered-client repository
     */
    @Bean
    RegisteredClientRepository registeredClientRepository(PasswordEncoder passwordEncoder) {
        RegisteredClient client = RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId("demo-client")
                .clientSecret(passwordEncoder.encode("demo-secret"))
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                .redirectUri("http://127.0.0.1:8080/login/oauth2/code/demo-client")
                .scope("api.read")
                .scope("api.write")
                .scope("openid")
                .scope("profile")
                .clientSettings(ClientSettings.builder()
                        .requireAuthorizationConsent(false)
                        .requireProofKey(true)
                        .build())
                .tokenSettings(TokenSettings.builder()
                        .accessTokenTimeToLive(Duration.ofMinutes(15))
                        .reuseRefreshTokens(false)
                        .build())
                .build();
        return new InMemoryRegisteredClientRepository(client);
    }

    /**
     * Provides a delegating password encoder for client-secret storage.
     *
     * @return the configured password encoder
     */
    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    /**
     * Creates an ephemeral 2048-bit RSA key pair used to sign and verify tokens.
     * A new key is generated at each application startup.
     *
     * @return the RSA signing key and corresponding public key
     * @throws Exception if the RSA key pair cannot be generated
     */
    @Bean
    RSAKey rsaSigningKey() throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        return new RSAKey.Builder((RSAPublicKey) keyPair.getPublic())
                .privateKey((RSAPrivateKey) keyPair.getPrivate())
                .keyID(UUID.randomUUID().toString())
                .build();
    }

    /**
     * Publishes the signing key as a JWK so authorization-server clients can
     * retrieve the public verification key from the JWKS endpoint.
     *
     * @param rsaSigningKey the key used to sign access tokens
     * @return a JWK source backed by the signing key
     */
    @Bean
    JWKSource<SecurityContext> jwkSource(RSAKey rsaSigningKey) {
        JWKSet jwkSet = new JWKSet(rsaSigningKey);
        return (selector, context) -> selector.select(jwkSet);
    }

    /**
     * Creates a JWT decoder that verifies signatures and standard time claims,
     * and rejects tokens whose issuer does not match this server.
     *
     * @param rsaSigningKey key whose public half verifies JWT signatures
     * @param authorizationServerSettings settings containing the trusted issuer
     * @return a validating JWT decoder
     * @throws com.nimbusds.jose.JOSEException if the public key cannot be extracted
     */
    @Bean
    JwtDecoder jwtDecoder(
            RSAKey rsaSigningKey,
            AuthorizationServerSettings authorizationServerSettings) throws com.nimbusds.jose.JOSEException {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey(rsaSigningKey.toRSAPublicKey()).build();
        decoder.setJwtValidator(
                JwtValidators.createDefaultWithIssuer(authorizationServerSettings.getIssuer()));
        return decoder;
    }

    /**
     * Sets the issuer and protocol endpoint configuration for this server.
     *
     * @param issuer externally visible issuer URL
     * @return the authorization-server settings
     */
    @Bean
    AuthorizationServerSettings authorizationServerSettings(@Value("${oauth2.issuer}") String issuer) {
        return AuthorizationServerSettings.builder()
                .issuer(issuer)
                .build();
    }
}
