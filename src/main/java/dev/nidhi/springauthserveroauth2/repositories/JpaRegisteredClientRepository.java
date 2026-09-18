package dev.nidhi.springauthserveroauth2.repositories;

import dev.nidhi.springauthserveroauth2.entities.RegisteredClientEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.stereotype.Component;

import org.springframework.stereotype.Repository;
import tools.jackson.databind.json.JsonMapper;

@Repository
public class JpaRegisteredClientRepository implements RegisteredClientRepository {

    private final ClientRepository clientRepository;
    private final JsonMapper jsonMapper;

    public JpaRegisteredClientRepository(
            ClientRepository repository,
            JsonMapper jsonMapper) {

        this.clientRepository = repository;
        this.jsonMapper = jsonMapper;
    }

    @Override
    public void save(RegisteredClient registeredClient) {

        RegisteredClientEntity registeredClientEntity =
                new RegisteredClientEntity();

        registeredClientEntity.setId(registeredClient.getId());
        registeredClientEntity.setClientId(registeredClient.getClientId());
        registeredClientEntity.setClientSecret(registeredClient.getClientSecret());
        registeredClientEntity.setClientName(registeredClient.getClientName());
        registeredClientEntity.setClientSecretExpiresAt(registeredClient.getClientSecretExpiresAt());
        registeredClientEntity.setClientIdIssuedAt(registeredClient.getClientIdIssuedAt());

        registeredClientEntity.setClientAuthenticationMethods(
                String.join(",",
                        registeredClient.getClientAuthenticationMethods()
                                .stream()
                                .map(clientAuthenticationMethod -> clientAuthenticationMethod.getValue())
                                .toList()
                )
        );

        registeredClientEntity.setAuthorizationGrantTypes(
                String.join(",",
                        registeredClient.getAuthorizationGrantTypes()
                                .stream()
                                .map(authorizationGrantType -> authorizationGrantType.getValue())
                                .toList()
                )
        );


        registeredClientEntity.setRedirectUris(
                String.join(",",
                        registeredClient.getRedirectUris()
                )
        );

        registeredClientEntity.setPostLogoutRedirectUris(
                String.join(",",
                        registeredClient.getPostLogoutRedirectUris()
                )
        );

        registeredClientEntity.setScopes(
                String.join(",",
                        registeredClient.getScopes()
                )
        );
      clientRepository.save(registeredClientEntity);
    }

    @Override
    public RegisteredClient findById(String id) {
        return clientRepository.findById(id)
                        .map(this::toRegisteredClient)
                        .orElse(null);

    }

    @Override
    public RegisteredClient findByClientId(String clientId) {
        return clientRepository.findByClientId(clientId)
                .map(this::toRegisteredClient)
                .orElse(null);
    }

    private RegisteredClient toRegisteredClient(RegisteredClientEntity entity) {
        RegisteredClient.Builder builder = RegisteredClient
                .withId(entity.getId())
                .clientId(entity.getClientId())
                .clientIdIssuedAt(entity.getClientIdIssuedAt())
                .clientName(entity.getClientName())
                .clientSecret(entity.getClientSecret())
                .clientSecretExpiresAt(entity.getClientSecretExpiresAt());

        for (String method : entity.getClientAuthenticationMethods().split(",")) {
            builder.clientAuthenticationMethod(new ClientAuthenticationMethod(method));
        }

        for (String grantType: entity.getAuthorizationGrantTypes().split(",")) {
            builder.authorizationGrantType(new AuthorizationGrantType(grantType));
        }

        for(String scope : entity.getScopes().split(",")) {
            builder.scope(scope);
        }

        String postLogoutRedirectUris = entity.getPostLogoutRedirectUris();
        if(postLogoutRedirectUris != null && !postLogoutRedirectUris.isBlank()) {
            for(String postLogoutRedirect : entity.getPostLogoutRedirectUris().split(",")) {
                builder.postLogoutRedirectUri(postLogoutRedirect);
            }
        }

        for (String redirectUri : entity.getRedirectUris().split(",")) {
            builder.redirectUri(redirectUri);
        }

        return builder.build();
    }
}