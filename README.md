# OAuth2 Authorization Server and Product Service Security

## Overview

This project implements OAuth2-based authentication and authorization using two separate Spring Boot applications:

1. **Authorization Server** — authenticates users and issues OAuth2 access/refresh tokens.
2. **Product Service** — acts as an OAuth2 Resource Server and protects product APIs using JWT access tokens.

The Authorization Server runs on:

```text
http://localhost:8080
```

The Product Service runs on:

```text
http://localhost:8081
```

The two applications are independent services.

```text
                    ┌──────────────────────────┐
                    │    Authorization Server   │
                    │       localhost:8080      │
                    │                          │
                    │  • User authentication   │
                    │  • OAuth2 client storage │
                    │  • Token generation      │
                    │  • JWT signing           │
                    └────────────┬─────────────┘
                                 │
                                 │ JWT Access Token
                                 ▼
                    ┌──────────────────────────┐
                    │      Product Service      │
                    │       localhost:8081      │
                    │                          │
                    │  OAuth2 Resource Server │
                    │  • JWT validation       │
                    │  • Authentication       │
                    │  • Authorization        │
                    │  • Product APIs         │
                    └──────────────────────────┘
```

---

## Architecture

The system follows the standard OAuth2 architecture:

```text
                    OAuth2 Authorization Server
                              :8080
                                │
                                │ Issues JWT
                                ▼
                             Client
                           (Postman)
                                │
                                │ Bearer Token
                                ▼
                    OAuth2 Resource Server
                              :8081
                                │
                                ▼
                         Product APIs
```

### Responsibilities

### Authorization Server

The Authorization Server is responsible for:

* Authenticating users.
* Managing OAuth2 clients.
* Handling the Authorization Code flow.
* Supporting PKCE.
* Generating access tokens.
* Generating refresh tokens.
* Signing JWT access tokens.
* Storing OAuth2 authorization data.
* Storing user roles.

### Product Service

The Product Service is responsible for:

* Receiving Bearer access tokens.
* Validating JWT signatures.
* Validating the token issuer.
* Validating token expiration.
* Converting JWT authorities into Spring Security authorities.
* Applying role-based authorization rules.
* Serving protected product APIs.

The Product Service does not need to share the Authorization Server's database.

---

# OAuth2 Authorization Code + PKCE Flow

The application uses the OAuth2 Authorization Code flow with PKCE.

The high-level flow is:

```text
Postman
   │
   │ Authorization Request
   ▼
Authorization Server
   │
   │ Login
   ▼
Browser
   │
   │ Username + Password
   ▼
Authorization Server
   │
   │ Authorization Code
   ▼
Postman
   │
   │ Authorization Code + PKCE Verifier
   ▼
Authorization Server
   │
   │ Access Token + Refresh Token
   ▼
Postman
```

## 1. Authorization Request

Postman initiates an authorization request to:

```text
/oauth2/authorize
```

The request contains information such as:

* `client_id`
* `redirect_uri`
* `response_type=code`
* `scope`
* PKCE `code_challenge`
* `code_challenge_method`

The configured OAuth2 client is:

```text
postman-client
```

---

## 2. User Authentication

If the user does not already have an authenticated browser session, Spring Security displays the login page.

The user authenticates using credentials stored in the Authorization Server database.

Users currently configured for testing include:

```text
Nidhi     → ROLE_USER
Utkarsh   → ROLE_ADMIN
```

The application uses a custom `UserDetailsService` to load users from the database.

Database roles are converted to Spring Security authorities:

```text
USER
  ↓
ROLE_USER
```

and:

```text
ADMIN
  ↓
ROLE_ADMIN
```

---

## 3. Authorization Code

After successful authentication, the Authorization Server generates a short-lived authorization code.

The authorization code is not the access token.

It is a temporary credential that the client exchanges for tokens.

```text
User Authentication
        │
        ▼
Authorization Server
        │
        ▼
Authorization Code
        │
        ▼
Postman
```

---

## 4. Token Exchange

Postman sends the authorization code to:

```text
/oauth2/token
```

along with the PKCE verifier.

The Authorization Server validates the request and issues:

* Access token
* Refresh token

The access token is a signed JWT.

The current access-token lifetime is:

```text
5 minutes
```

The current refresh-token lifetime is:

```text
1 hour
```

---

# JWT Access Token

The Authorization Server adds user authorities to the JWT using an `OAuth2TokenCustomizer<JwtEncodingContext>`.

For a USER:

```json
{
  "sub": "nidhi@example.com",
  "authorities": [
    "ROLE_USER"
  ]
}
```

For an ADMIN:

```json
{
  "sub": "utkarsh",
  "authorities": [
    "ROLE_ADMIN"
  ]
}
```

The `authorities` claim is a custom claim added by the application.

---

# JWT Validation in Product Service

Product Service is configured as an OAuth2 Resource Server.

The configuration is:

```properties
spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost:8080
```

This tells Product Service that the Authorization Server is:

```text
http://localhost:8080
```

Spring Security uses the Authorization Server metadata and public signing key information to validate JWTs.

The private signing key remains with the Authorization Server.

```text
Authorization Server
        │
        ├── Private Key
        │      │
        │      └── Signs JWT
        │
        └── Public Key
               │
               └── Used by Product Service
                   to verify JWT
```

---

# JWT Validation Steps

When Product Service receives:

```http
Authorization: Bearer <JWT>
```

Spring Security performs JWT authentication.

Conceptually, it validates:

### 1. Signature

The signature must have been created by the trusted Authorization Server.

### 2. Issuer

The JWT issuer must match:

```text
http://localhost:8080
```

### 3. Expiration

The JWT must not be expired.

An expired or invalid JWT results in an authentication failure.

---

# Mapping JWT Authorities to Spring Security

The JWT contains:

```json
"authorities": [
  "ROLE_USER"
]
```

Spring Security's default JWT converter does not automatically use this custom claim.

Therefore, Product Service uses:

```java
JwtGrantedAuthoritiesConverter
```

with:

```java
grantedAuthoritiesConverter.setAuthoritiesClaimName(
    "authorities"
);
```

The application also uses:

```java
grantedAuthoritiesConverter.setAuthorityPrefix("");
```

because the JWT already contains the complete authority name:

```text
ROLE_USER
```

We do not want Spring to modify it.

The resulting flow is:

```text
JWT
 │
 └── authorities: ["ROLE_USER"]
              │
              ▼
JwtGrantedAuthoritiesConverter
              │
              ▼
Spring GrantedAuthority
              │
              ▼
ROLE_USER
```

For the ADMIN user:

```text
JWT
 │
 └── authorities: ["ROLE_ADMIN"]
              │
              ▼
Spring GrantedAuthority
              │
              ▼
ROLE_ADMIN
```

---

# Authentication vs Authorization

These are two different security concepts.

## Authentication

Authentication answers:

> Who is the user?

For example:

```text
Nidhi
ROLE_USER
```

or:

```text
Utkarsh
ROLE_ADMIN
```

## Authorization

Authorization answers:

> Is this authenticated user allowed to perform this operation?

For example:

```text
ROLE_USER
    ↓
GET /products
    ↓
Allowed
```

while:

```text
ROLE_USER
    ↓
POST /products
    ↓
ROLE_ADMIN required
    ↓
Forbidden
```

---

# Role-Based Authorization

Product Service currently implements the following authorization rules:

| HTTP Method | Endpoint       | Required Role |
| ----------- | -------------- | ------------- |
| GET         | `/products/**` | USER or ADMIN |
| POST        | `/products/**` | ADMIN         |
| PUT         | `/products/**` | ADMIN         |
| DELETE      | `/products/**` | ADMIN         |

The configuration uses:

```java
.requestMatchers(HttpMethod.GET, "/products/**")
    .hasAnyRole("USER", "ADMIN")

.requestMatchers(HttpMethod.POST, "/products/**")
    .hasRole("ADMIN")

.requestMatchers(HttpMethod.PUT, "/products/**")
    .hasRole("ADMIN")

.requestMatchers(HttpMethod.DELETE, "/products/**")
    .hasRole("ADMIN")
```

`hasRole("ADMIN")` checks for:

```text
ROLE_ADMIN
```

because Spring Security automatically applies the `ROLE_` prefix for `hasRole()`.

---

# Request Lifecycle

A protected Product Service request follows this flow:

```text
Client
  │
  │ Authorization: Bearer <JWT>
  ▼
Product Service
  │
  ▼
Extract JWT
  │
  ▼
Validate JWT signature
  │
  ▼
Validate issuer
  │
  ▼
Validate expiration
  │
  ▼
Create Authentication
  │
  ▼
Read "authorities" claim
  │
  ▼
ROLE_USER / ROLE_ADMIN
  │
  ▼
Authorization Rules
  │
  ├───────────────┐
  │               │
  ▼               ▼
Allowed         Forbidden
  │               │
  ▼               ▼
Controller        403
  │
  ▼
Service
  │
  ▼
Repository
  │
  ▼
Database
```

---

# HTTP 401 vs HTTP 403

The application distinguishes between authentication and authorization failures.

## 401 Unauthorized

A `401` generally indicates that the request does not contain valid authentication.

Examples:

* No access token.
* Invalid JWT.
* Expired JWT.
* Invalid signature.
* Invalid issuer.

Flow:

```text
Request
   ↓
No valid authentication
   ↓
401 Unauthorized
```

## 403 Forbidden

A `403` means authentication succeeded but the authenticated user does not have sufficient authority.

Example:

```text
Nidhi
ROLE_USER
   ↓
POST /products
   ↓
ROLE_ADMIN required
   ↓
403 Forbidden
```

The user is authenticated, but authorization fails.

---

# Testing

## USER Token

Nidhi's token contains:

```json
"authorities": [
  "ROLE_USER"
]
```

Expected behavior:

```text
GET     /products       → 200 OK
POST    /products       → 403 Forbidden
PUT     /products/{id}  → 403 Forbidden
DELETE  /products/{id}  → 403 Forbidden
```

## ADMIN Token

Utkarsh's token contains:

```json
"authorities": [
  "ROLE_ADMIN"
]
```

Expected behavior:

```text
GET     /products       → 200 OK
POST    /products       → 200 OK
PUT     /products/{id}  → 200 OK
DELETE  /products/{id}  → 200 OK
```

Both users were tested successfully.

---

# Browser Session vs Access Token

The browser login session and the OAuth2 access token are separate concepts.

The browser session is used by the Authorization Server to remember that the user is authenticated.

The access token is issued after successful authorization and is subsequently used by the Resource Server.

Therefore, logging out of the browser session does not automatically invalidate an already-issued JWT access token in the current implementation.

The access token remains valid until its expiration or until additional token-revocation mechanisms are implemented.

Current access-token lifetime:

```text
5 minutes
```

---

# Persistence

The Authorization Server persists OAuth2-related information using:

### Registered Clients

Stores OAuth2 client information such as:

* Client ID
* Client secret
* Redirect URIs
* Grant types
* Scopes
* Client settings
* Token settings

### OAuth2 Authorization

Stores authorization/token-related state required by the Authorization Server.

### OAuth2 Authorization Consent

Stores authorization consent information when consent is required.

### Users

Stores application users:

```text
users
--------------------------------
email               role
--------------------------------
nidhi@example.com   USER
utkarsh             ADMIN
```

Passwords are stored using BCrypt hashing.

---

# Security Components

## Authorization Server

Main responsibilities:

```text
Spring Authorization Server
Spring Security
JPA
JDBC OAuth2 Authorization persistence
Flyway
MySQL
JWT
PKCE
```

## Product Service

Main security components:

```text
Spring Security
OAuth2 Resource Server
JWT
JwtAuthenticationConverter
JwtGrantedAuthoritiesConverter
```

---

# Key Security Principle

The most important architectural principle in this implementation is the separation of responsibilities:

```text
Authorization Server
        │
        │ "Who are you?"
        │
        │ "Here is your signed token."
        ▼
       JWT
        │
        ▼
Resource Server
        │
        │ "Is this token valid?"
        │
        │ "What authorities does this user have?"
        │
        │ "Is this authority allowed to access this endpoint?"
        ▼
Protected API
```

The Authorization Server does not need to be contacted for every API request.

Once Product Service has the JWT and the information required to validate it, it can authenticate and authorize the request locally.

---

# Current Status

The following functionality has been implemented and tested:

* OAuth2 Authorization Server
* Authorization Code flow
* PKCE
* Form-based user authentication
* Database-backed users
* BCrypt password hashing
* USER and ADMIN roles
* JWT access tokens
* Custom `authorities` JWT claim
* Refresh tokens
* Persistent OAuth2 authorization data
* Product Service as OAuth2 Resource Server
* JWT signature validation
* JWT issuer validation
* JWT expiration validation
* JWT authority extraction
* Role-based API authorization
* USER vs ADMIN access control
* 401 authentication failure handling
* 403 authorization failure handling

The resulting architecture separates **identity/token issuance** from **API resource protection**, following the standard OAuth2 Resource Server model.
















<img width="1264" height="759" alt="image" src="https://github.com/user-attachments/assets/a59625c1-3b46-4b7e-8463-cd11cf49e341" />
<img width="1264" height="729" alt="image" src="https://github.com/user-attachments/assets/41bc2d67-c43c-4015-b6ba-6c3d44811d07" />
<img width="1275" height="750" alt="image" src="https://github.com/user-attachments/assets/005eb32b-2b32-47dd-924d-a8642618c063" />
<img width="1266" height="744" alt="image" src="https://github.com/user-attachments/assets/a89ad6a7-24af-4e53-b649-53e54cf21ab3" />
Generate access token, give the user and password, then token will be generated
<img width="994" height="478" alt="image" src="https://github.com/user-attachments/assets/10613438-9523-4c06-9914-962b07828ec7" />
