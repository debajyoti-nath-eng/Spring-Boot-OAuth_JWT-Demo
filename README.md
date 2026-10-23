# OAuth 2.0 + JWT REST API

A minimal Spring Boot application that runs an OAuth 2.0 Authorization Server and a JWT-protected REST API in one service. OAuth2 access tokens are signed as JWTs; Spring Security validates their signature and standard time claims before allowing access to protected API routes.

## Run

Requires Java 17 or newer and Maven.

```shell
mvn spring-boot:run
```

The server listens on `http://localhost:9000`.

## Get a JWT

The included `demo-client` supports the OAuth 2.0 client credentials grant:

```shell
curl -u demo-client:demo-secret \
  -d "grant_type=client_credentials&scope=api.read" \
  http://localhost:9000/oauth2/token
```

The response contains an `access_token` JWT. Use that value as a bearer token:

```shell
curl -H "Authorization: Bearer <access_token>" \
  http://localhost:9000/api/profile
```

`GET /api/public` is public. `GET /api/profile` requires a valid bearer JWT.
The server also supports the authorization code and refresh token grants for
`demo-client`; authorization code requests require PKCE and the registered
redirect URI `http://127.0.0.1:8080/login/oauth2/code/demo-client`. The
authorization login uses the example user `demo-user` with password `changeit`.

## Project layout

```text
src/main/java/com/example/oauthjwt/
  OAuthJwtApiApplication.java
  SecurityConfiguration.java
  ApiController.java
src/main/resources/application.properties
src/test/java/com/example/oauthjwt/OAuthJwtApiIntegrationTest.java
```

The client and user are in-memory demo credentials, and an RSA signing key is
generated each time the application starts. Replace these with securely managed
client/user storage and a persistent signing key before deploying; changing the
key invalidates previously issued access tokens. Serve the application over
HTTPS and keep client secrets out of source control in production.
