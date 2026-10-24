# OAuth 2.0, OpenID Connect, and JWT Demo

A Spring Boot application demonstrating the distinct roles of an OAuth 2.0
**Authorization Server**, an OpenID Connect **Identity Provider**, and an OAuth
2.0 **Resource Server**. They run in one process for learning purposes; the
package boundaries make each responsibility visible.

OAuth 2.0 is the authorization framework: it defines how a client obtains a
limited access token. OpenID Connect adds user authentication and identity
claims on top of OAuth 2.0. JWT is a token format used here for signed access
tokens; a JWT is not itself an OAuth grant or proof that a user was
authenticated.

## Requirements and startup

- Java 17 or newer
- Maven

```shell
mvn spring-boot:run
```

The default issuer and application URL are `http://localhost:9000`. Configure
the issuer using `OAUTH2_ISSUER` when the server is exposed under a different
externally visible URL. The issuer configured here must match the `iss` claim
in its tokens and must be trusted by resource servers.

## Request a client-credentials token

The registered `demo-client` supports the OAuth 2.0 client-credentials grant:

```shell
curl -u demo-client:demo-secret \
  -d "grant_type=client_credentials&scope=api.read" \
  http://localhost:9000/oauth2/token
```

The response includes an `access_token`, which can be sent to the protected
profile endpoint:

```shell
curl -H "Authorization: Bearer <access_token>" \
  http://localhost:9000/api/profile
```

Client credentials represents the client, not an end user. For an interactive
user sign-in demonstration, the client also supports authorization code with
PKCE and the `openid`/`profile` scopes. The example user is `demo-user` with
password `changeit`; the registered redirect URI is
`http://127.0.0.1:8080/login/oauth2/code/demo-client`.

## API and protocol endpoints

| Endpoint | Access | Purpose |
| --- | --- | --- |
| `POST /oauth2/token` | Registered client | Issues access tokens for supported grants |
| `GET /oauth2/jwks` | Public | Publishes public signing keys for JWT verification |
| `GET /.well-known/openid-configuration` | Public | OpenID Connect provider metadata |
| `GET /userinfo` | Valid OIDC access token | Returns identity claims for an authenticated user |
| `GET /api/public` | Public | Example unauthenticated resource |
| `GET /api/profile` | `api.read` scope | Example resource showing the authenticated subject |
| `GET /api/admin/status` | `api.write` scope | Example privileged resource |

The authorization server also provides the standard authorization and token
protocol endpoints used in the authorization-code flow.

## JWT validation and authorization

The authorization server signs tokens with an RSA key. The resource-server
filter chain accepts bearer tokens and validates their signature, standard time
claims (including expiration), and issuer. Spring Security maps granted OAuth
scopes to authorities with the `SCOPE_` prefix. The route rules then require
`SCOPE_api.read` or `SCOPE_api.write`; a valid token without the required scope
is forbidden, while a missing or invalid token is unauthorized.

## Project structure

```text
src/main/java/com/example/oauthjwt/
  OAuthJwtApiApplication.java
  api/
    ApiController.java
  config/
    AuthorizationServerConfiguration.java
    ResourceServerConfiguration.java
  identity/
    IdentityConfiguration.java
src/main/resources/
  application.properties
src/test/java/com/example/oauthjwt/
  OAuthJwtApiIntegrationTest.java
docs/
  architecture.md
```

See [Architecture and request flows](docs/architecture.md) for the component
responsibilities, startup wiring, and token/request sequence diagrams.

## Demonstration-only limitations

The OAuth client and user are held in memory; the RSA key pair is generated
again at every startup, so restarting invalidates previously issued tokens.
Client credentials and sample user credentials are public examples, not
production secrets. Before deployment, use persistent client and identity
storage, securely provision and rotate signing keys, keep secrets out of source
control, use HTTPS, and configure a stable external issuer URL. If the
authorization server and resource server are split into separate services,
configure the resource server to retrieve the issuer's trusted public keys
from its issuer/JWKS metadata instead of sharing an in-process key bean.
