package dev.nidhi.springauthserveroauth2.services;

import dev.nidhi.springauthserveroauth2.entities.OAuth2AuthorizationConsentEntity;
import dev.nidhi.springauthserveroauth2.repositories.OAuth2AuthorizationConsentRepository;

import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsent;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.stereotype.Service;

import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Arrays;
import java.util.stream.Collectors;

@Service
public class JpaOAuth2AuthorizationConsentService
        implements OAuth2AuthorizationConsentService {

    private final OAuth2AuthorizationConsentRepository repository;

    public JpaOAuth2AuthorizationConsentService(
            OAuth2AuthorizationConsentRepository repository) {

        this.repository = repository;
    }

    @Override
    public void save(OAuth2AuthorizationConsent consent) {

        OAuth2AuthorizationConsentEntity entity =
                new OAuth2AuthorizationConsentEntity();

        entity.setRegisteredClientId(
                consent.getRegisteredClientId()
        );

        entity.setPrincipalName(
                consent.getPrincipalName()
        );

        entity.setAuthorities(
                consent.getAuthorities()
                        .stream()
                        .map(authority -> authority.getAuthority())
                        .collect(Collectors.joining(","))
        );

        repository.save(entity);
    }

    @Override
    public void remove(
            OAuth2AuthorizationConsent consent) {

        OAuth2AuthorizationConsentEntity entity =
                new OAuth2AuthorizationConsentEntity();

        entity.setRegisteredClientId(
                consent.getRegisteredClientId()
        );

        entity.setPrincipalName(
                consent.getPrincipalName()
        );

        repository.deleteById(
                new dev.nidhi.springauthserveroauth2.entities
                        .OAuth2AuthorizationConsentId(
                        entity.getRegisteredClientId(),
                        entity.getPrincipalName()
                )
        );
    }

    @Override
    public OAuth2AuthorizationConsent findById(
            String registeredClientId,
            String principalName) {

        return repository.findById(
                        new dev.nidhi.springauthserveroauth2.entities
                                .OAuth2AuthorizationConsentId(
                                registeredClientId,
                                principalName
                        )
                )
                .map(this::toOAuth2AuthorizationConsent)
                .orElse(null);
    }

    private OAuth2AuthorizationConsent toOAuth2AuthorizationConsent(
            OAuth2AuthorizationConsentEntity entity) {

        OAuth2AuthorizationConsent.Builder builder =
                OAuth2AuthorizationConsent
                        .withId(
                                entity.getRegisteredClientId(),
                                entity.getPrincipalName()
                        );

        if (entity.getAuthorities() != null
                && !entity.getAuthorities().isBlank()) {

            Arrays.stream(entity.getAuthorities().split(","))
                    .map(SimpleGrantedAuthority::new)
                    .forEach(builder::authority);
        }

        return builder.build();
    }
}
