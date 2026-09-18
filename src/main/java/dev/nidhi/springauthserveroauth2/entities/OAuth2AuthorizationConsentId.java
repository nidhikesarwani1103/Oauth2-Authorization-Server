package dev.nidhi.springauthserveroauth2.entities;

import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class OAuth2AuthorizationConsentId implements Serializable {

    private String registeredClientId;
    private String principalName;
}
