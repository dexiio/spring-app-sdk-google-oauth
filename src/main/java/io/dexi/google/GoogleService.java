package io.dexi.google;


import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.CredentialRefreshListener;
import com.google.api.client.auth.oauth2.TokenErrorResponse;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeRequestUrl;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.DataStoreFactory;
import com.google.api.client.util.store.MemoryDataStoreFactory;
import com.google.api.services.plus.Plus;
import com.google.api.services.plus.model.Person;
import io.dexi.oauth.OAuth2Tokens;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URL;
import java.security.GeneralSecurityException;

@Slf4j
@Service
@DependsOn("dexiConfigWrapper")
public class GoogleService {

    private final DataStoreFactory dataStoreFactory = new MemoryDataStoreFactory();

    private final JacksonFactory jacksonFactory = new JacksonFactory();

    private final NetHttpTransport transport;

    private final GoogleOAuthProperties properties;

    @Autowired
    public GoogleService(GoogleOAuthProperties properties) throws GeneralSecurityException, IOException {
        this.transport = GoogleNetHttpTransport.newTrustedTransport();
        this.properties = properties;
        properties.validate();

        log.info("Initialising Google Service with app name: {}", properties.getAppName());
        log.info("- Google Scopes: {}", properties.getScopes());
        log.info("- Google Client Id: {}", properties.getClientId());
    }

    public URL getRedirectUrl(String state, String returnUrl) {
        GoogleAuthorizationCodeRequestUrl authorizationCodeRequestUrl = new GoogleAuthorizationCodeRequestUrl(
                properties.getClientId(),
                returnUrl,
                properties.getScopes()
        );

        authorizationCodeRequestUrl.setAccessType("offline");
        authorizationCodeRequestUrl.setApprovalPrompt("force");
        authorizationCodeRequestUrl.setState(state);

        return authorizationCodeRequestUrl.toURL();
    }

    public GoogleTokenResponse getTokenResponse(String code, String redirectUri) throws IOException {
        return new GoogleAuthorizationCodeTokenRequest(
                transport,
                jacksonFactory,
                properties.getClientId(),
                properties.getSecret(),
                code,
                redirectUri
        ).execute();
    }

    public Person getUser(OAuth2Tokens tokens) throws IOException {
        Plus plus = createClient(tokens);

        return plus.people().get("me").execute();

    }

    private Plus createClient(OAuth2Tokens tokens) {

        final Credential credentials = createCredentials(tokens);

        return new Plus.Builder(transport, jacksonFactory, credentials)
                .setApplicationName(properties.getAppName())
                .build();
    }

    private GoogleAuthorizationCodeFlow.Builder createAuthFlowBuilder() throws IOException {
        return new GoogleAuthorizationCodeFlow.Builder(
                transport,
                jacksonFactory,
                properties.getClientId(),
                properties.getSecret(),
                properties.getScopes()
        ).setDataStoreFactory(dataStoreFactory);
    }

    private GoogleAuthorizationCodeFlow createAuthFlow() throws IOException {
        return createAuthFlowBuilder().build();
    }

    private GoogleAuthorizationCodeFlow createAuthFlow(OAuth2Tokens config) throws IOException {
        return createAuthFlowBuilder()
                .addRefreshListener(new CredentialRefreshListener() {
                    @Override
                    public void onTokenResponse(Credential credential, TokenResponse tokenResponse) throws IOException {
                        config.setAccessToken(credential.getAccessToken());
                        config.setRefreshToken(credential.getRefreshToken());
                    }

                    @Override
                    public void onTokenErrorResponse(Credential credential, TokenErrorResponse tokenErrorResponse) throws IOException {
                        config.setValid(false);
                    }

                }).build();
    }

    private TokenResponse toTokenResponse(OAuth2Tokens config) {
        final TokenResponse tokenResponse = new TokenResponse();
        tokenResponse.setAccessToken(config.getAccessToken());
        tokenResponse.setRefreshToken(config.getRefreshToken());
        tokenResponse.setTokenType("Bearer");
        tokenResponse.setExpiresInSeconds(config.getExpiresInSeconds());
        tokenResponse.setScope(config.getScope());
        return tokenResponse;
    }

    public Credential createCredentials(OAuth2Tokens config) {

        final GoogleAuthorizationCodeFlow authFlow;
        try {
            authFlow = createAuthFlow(config);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to create Google OAuth flow", e);
        }

        final TokenResponse tokenResponse = toTokenResponse(config);

        Credential.Builder credentialBuilder = new Credential.Builder(authFlow.getMethod())
                .setTransport(authFlow.getTransport())
                .setJsonFactory(authFlow.getJsonFactory())
                .setTokenServerEncodedUrl(authFlow.getTokenServerEncodedUrl())
                .setClientAuthentication(authFlow.getClientAuthentication())
                .setRequestInitializer(authFlow.getRequestInitializer())
                .setClock(authFlow.getClock());

        return credentialBuilder
                .build()
                .setFromTokenResponse(tokenResponse);
    }

    public JacksonFactory getJacksonFactory() {
        return jacksonFactory;
    }

    public NetHttpTransport getTransport() {
        return transport;
    }
}
