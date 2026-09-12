package dev.nidhi.springauthserveroauth2.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "oauth2_registered_client")
@Getter
@Setter
public class RegisteredClientEntity {

    @Id
    private String id;

    @Column(nullable = false, unique = true)
    private String clientId;

    @Column(nullable = false)
    private Instant clientIdIssuedAt;

    private String clientSecret;

    private Instant clientSecretExpiresAt;

    @Column(nullable = false)
    private String clientName;

    @Column(nullable = false, length = 1000)
    private String clientAuthenticationMethods;

    @Column(nullable = false, length = 1000)
    private String authorizationGrantTypes;

    @Column(nullable = false, length = 2000)
    private String redirectUris;

    @Column(length = 2000)
    private String postLogoutRedirectUris;

    @Column(nullable = false, length = 1000)
    private String scopes;

    @Column(length = 2000)
    private String clientSettings;

    @Column(length = 2000)
    private String tokenSettings;
}