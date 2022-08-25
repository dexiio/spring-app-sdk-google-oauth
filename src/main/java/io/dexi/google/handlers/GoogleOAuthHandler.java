package io.dexi.google.handlers;

import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.plus.model.Person;
import io.dexi.google.GoogleService;
import io.dexi.oauth.OAuth2Tokens;
import io.dexi.service.exceptions.UserErrorException;
import io.dexi.service.handlers.OAuth2Handler;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.URL;

@Slf4j
@Service
public class GoogleOAuthHandler implements OAuth2Handler {

    private static final String OAUTH_PROVIDER = "google";

    private static final String OAUTH_PROVIDER_NAME = "Google";

    private final GoogleService googleService;

    @Autowired
    public GoogleOAuthHandler(GoogleService googleService) {
        this.googleService = googleService;
    }

    public URL getRedirectUrl(String state, String returnUrl) {
        return googleService.getRedirectUrl(state, returnUrl);
    }


    @Override
    public String getProviderName() {
        return OAUTH_PROVIDER_NAME;
    }


    @Override
    public OAuth2Tokens validate(String code, String redirectUri) {
        try {
            final GoogleTokenResponse response = googleService.getTokenResponse(code, redirectUri);

            if (response.isEmpty()) {
                return null;
            }

            OAuth2Tokens out = new OAuth2Tokens();
            out.setProvider(OAUTH_PROVIDER);
            out.setValid(true);
            out.setAccessToken(response.getAccessToken());
            out.setRefreshToken(response.getRefreshToken());
            out.setExpiresInSeconds(response.getExpiresInSeconds());
            out.setScope(response.getScope());

            final Person user = googleService.getUser(out, false);

            for(Person.Emails email : user.getEmails()) {
                if ("account".equalsIgnoreCase(email.getType())) {
                    out.setEmail(email.getValue());
                }
            }

            if (StringUtils.isBlank(user.getDisplayName())) {
                out.setName(out.getEmail());
            } else {
                out.setName(user.getDisplayName());
            }

            return out;
        } catch(GoogleJsonResponseException e) {
            throw new ResponseStatusException(HttpStatus.valueOf(e.getStatusCode()), e.getDetails().getMessage());
        } catch (IOException e) {
            throw new UserErrorException("Error occurred while authenticating with Google", e);
        }
    }


}
