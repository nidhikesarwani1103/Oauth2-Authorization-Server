package dev.nidhi.springauthserveroauth2.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "oauth2_authorization_consent")
@IdClass(OAuth2AuthorizationConsentId.class)
@Getter
@Setter
public class OAuth2AuthorizationConsentEntity {

    @Id
    private String registeredClientId;

    @Id
    private String principalName;

    private String authorities;
}
