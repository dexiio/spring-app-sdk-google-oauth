package io.dexi.google;

import org.apache.commons.lang.StringUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "google")
@DependsOn("dexiConfigWrapper")
public class GoogleOAuthProperties {

    @NotBlank
    private String appName = "dexi.io/1.0";

    @NotEmpty
    private List<String> scopes;

    @NotBlank
    private String clientId;

    @NotBlank
    private String secret;

    @NotBlank
    private String appKey;

    private String mozendaClientId;
    private String mozendaSecret;
    
    void validate() {
        if (StringUtils.isBlank(appName)) {
            throw new IllegalArgumentException("Missing required configuration property: google.app-name");
        }

        if (scopes == null || scopes.isEmpty()) {
            throw new IllegalArgumentException("Missing required configuration property: google.scopes");
        }

        if (StringUtils.isBlank(clientId)) {
            throw new IllegalArgumentException("Missing required configuration property: google.client-id");
        }

        if (StringUtils.isBlank(secret)) {
            throw new IllegalArgumentException("Missing required configuration property: google.secret");
        }

        if (StringUtils.isBlank(appKey)) {
            throw new IllegalArgumentException("Missing required configuration property: google.app-key");
        }

        if (StringUtils.isNotEmpty(mozendaClientId) && StringUtils.isEmpty(mozendaSecret)) {
            throw new IllegalArgumentException("Missing required configuration property: google.mozenda-secret");
        }
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public List<String> getScopes() {
        return scopes;
    }

    public void setScopes(List<String> scopes) {
        this.scopes = scopes;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getMozendaClientId() {
        return mozendaClientId;
    }

    public void setMozendaClientId(String clientId) {
        this.mozendaClientId = clientId;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public String getMozendaSecret() {
        return mozendaSecret;
    }

    public void setMozendaSecret(String secret) {
        this.mozendaSecret = secret;
    }

    public String getAppKey() {
        return appKey;
    }

    public void setAppKey(String appKey) {
        this.appKey = appKey;
    }
}
