package dev.nidhi.springauthserveroauth2.configs;


import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import dev.nidhi.springauthserveroauth2.entities.RegisteredClientEntity;
import dev.nidhi.springauthserveroauth2.entities.UserEntity;
import dev.nidhi.springauthserveroauth2.repositories.ClientRepository;
import dev.nidhi.springauthserveroauth2.repositories.JpaRegisteredClientRepository;
import dev.nidhi.springauthserveroauth2.repositories.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.security.web.SecurityFilterChain;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;

import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;


import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;
import tools.jackson.databind.json.JsonMapper;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    @Order(1)
    public SecurityFilterChain authorizationServerSecurityFilterChain(
            HttpSecurity http, OAuth2AuthorizationService authorizationService) throws Exception {


        http
                .oauth2AuthorizationServer(authorizationServer -> {

                    http.securityMatcher(
                            authorizationServer.getEndpointsMatcher()
                    );

                    authorizationServer.oidc(Customizer.withDefaults());
                    authorizationServer.authorizationService(authorizationService);
                })
                .authorizeHttpRequests(auth ->
                        auth.anyRequest().authenticated()
                )
                .exceptionHandling(exceptions ->
                        exceptions.defaultAuthenticationEntryPointFor(
                                new LoginUrlAuthenticationEntryPoint("/login"),
                                new MediaTypeRequestMatcher(MediaType.TEXT_HTML)
                        )
                );

        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain defaultSecurityFilterChain(
            HttpSecurity http) throws Exception {

        http
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().authenticated()
                )
                .formLogin(Customizer.withDefaults());

        return http.build();
    }

//    @Bean
//    public UserDetailsService userDetailsService() {
//
//        UserDetails user = User.withUsername("nidhi")
//                .password("{noop}password")
//                .roles("USER")
//                .build();
//
//        return new InMemoryUserDetailsManager(user);
//    }

    @Bean
    public AuthorizationServerSettings authorizationServerSettings() {
        return AuthorizationServerSettings.builder()
                .issuer("http://localhost:8080")
                .build();
    }


    @Bean
    public JWKSource<SecurityContext> jwkSource() {
        RSAKey rsaKey = generateRsaKey();

        JWKSet jwkSet = new JWKSet(rsaKey);

        return new ImmutableJWKSet<>(jwkSet);
    }

    private static RSAKey generateRsaKey() {
        KeyPair keyPair = generateRsaKeyPair();

        return new RSAKey.Builder((RSAPublicKey) keyPair.getPublic())
                .privateKey((RSAPrivateKey) keyPair.getPrivate())
                .keyID(UUID.randomUUID().toString())
                .build();
    }

    private static KeyPair generateRsaKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    @Bean
    public JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource) {
        return OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource);
    }

    @Bean
    public OAuth2AuthorizationService authorizationService(
            JdbcOperations jdbcOperations, //this is not an issue, the code runs
            RegisteredClientRepository registeredClientRepository) {

        return new JdbcOAuth2AuthorizationService(
                jdbcOperations,
                registeredClientRepository
        );
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationProvider authenticationProvider(
            UserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder) {

        DaoAuthenticationProvider provider =
                new DaoAuthenticationProvider(
                        userDetailsService
                );

        provider.setPasswordEncoder(passwordEncoder);

        return provider;
    }

    // to create a user for the first time, we can do this using test case as well
    @Bean
    public CommandLineRunner createUser(UserRepository userRepository,
                                        PasswordEncoder passwordEncoder) {

        return args -> {

            if (userRepository.findByEmail("utkarsh@example.com").isEmpty()) {

                UserEntity user = new UserEntity();

                user.setEmail("utkarsh@example.com");

                user.setPassword(
                        passwordEncoder.encode("password123")
                );

                user.setRole("ADMIN");
                user.setEnabled(true);

                userRepository.save(user);

                System.out.println(
                        "Test user created: utkarsh@example.com"
                );
            }
        };
    }

    @Bean
    public CommandLineRunner createRegisteredClient(
            RegisteredClientRepository registeredClientRepository,
            PasswordEncoder passwordEncoder) {

        return args -> {

            if (registeredClientRepository.findByClientId("postman-client") == null) {

                RegisteredClient registeredClient = RegisteredClient
                        .withId(UUID.randomUUID().toString())
                        .clientId("postman-client")
                        .clientIdIssuedAt(Instant.now())

                        // Encode the OAuth client secret ONCE
                        .clientSecret(passwordEncoder.encode("postman-secret"))

                        .clientName("Postman Client")

                        .clientAuthenticationMethod(
                                ClientAuthenticationMethod.CLIENT_SECRET_BASIC
                        )

                        .authorizationGrantType(
                                AuthorizationGrantType.AUTHORIZATION_CODE
                        )
                        .authorizationGrantType(
                                AuthorizationGrantType.REFRESH_TOKEN
                        )

                        .redirectUri("https://oauth.pstmn.io/v1/callback")

                        .scope(OidcScopes.OPENID)
                        .scope(OidcScopes.PROFILE)
                        .scope("product.read")
                        .scope("product.write")

                        .clientSettings(
                                ClientSettings.builder()
                                        .requireAuthorizationConsent(false)
                                        .requireProofKey(true)
                                        .build()
                        )

                        .tokenSettings(
                                TokenSettings.builder()
                                        .build()
                        )

                        .build();

                registeredClientRepository.save(registeredClient);

                System.out.println("OAuth2 client created: postman-client");
            }
        };
    }


    @Bean
    public OAuth2TokenCustomizer<JwtEncodingContext> jwtTokenCustomizer() {

        return context -> {

            if (OAuth2TokenType.ACCESS_TOKEN.equals(context.getTokenType())) {

                Authentication authentication = context.getPrincipal();

                List<String> authorities = authentication.getAuthorities()
                        .stream()
                        .map(GrantedAuthority::getAuthority)
                        .filter(authority -> authority.startsWith("ROLE_"))
                        .toList();

                context.getClaims()
                        .claim("authorities", authorities);
            }
        };
    }

}
