package dev.nidhi.springauthserveroauth2.repositories;

import dev.nidhi.springauthserveroauth2.entities.OAuth2AuthorizationConsentEntity;
import dev.nidhi.springauthserveroauth2.entities.OAuth2AuthorizationConsentId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OAuth2AuthorizationConsentRepository
        extends JpaRepository<
        OAuth2AuthorizationConsentEntity,
        OAuth2AuthorizationConsentId> {
}
