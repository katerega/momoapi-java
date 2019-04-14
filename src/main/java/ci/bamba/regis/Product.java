package ci.bamba.regis;

import java.util.Base64;
import java.util.HashMap;
import java.util.UUID;

import ci.bamba.regis.exceptions.RequestException;
import ci.bamba.regis.models.ApiCredentials;
import ci.bamba.regis.models.ApiUser;
import ci.bamba.regis.models.Token;
import io.reactivex.Observable;

public class Product {

    private String baseUrl;
    private String subscriptionKey;
    private Environment environment;

    Product(String baseUrl, Environment environment, String subscriptionKey) {
        this.baseUrl = baseUrl;
        this.environment = environment;
        this.subscriptionKey = subscriptionKey;
    }

    public String getSubscriptionKey() {
        return subscriptionKey;
    }
    public Environment getEnvironment() {
        return environment;
    }
    public String getBaseUrl() {
        return this.baseUrl;
    }

    public Observable<String> createApiUser(String providerCallbackHost) {
        String referenceId = UUID.randomUUID().toString();
        HashMap<String, String> body = new HashMap<>();
        body.put("providerCallbackHost", providerCallbackHost);
        return RestClient
                .getService(getBaseUrl())
                .createApiUser(referenceId, getSubscriptionKey(), body)
                .map(response -> {
                    if (response.code() == 201) {
                        return referenceId;
                    } else {
                        throw new RequestException(response.code(), response.message());
                    }
                });
    }

    public Observable<ApiUser> getApiUser(String referenceId) {
        return RestClient.getService(getBaseUrl())
                .getApiUser(getSubscriptionKey(), referenceId)
                .map(response -> {
                    if (response.code() == 200) {
                        return response.body();
                    } else {
                        throw new RequestException(response.code(), response.message());
                    }
                });
    }

    public Observable<ApiCredentials> createApiKey(String referenceId) {
        return RestClient
                .getService(getBaseUrl())
                .createApiKey(getSubscriptionKey(), referenceId)
                .map(response -> {
                    if (response.code() == 201) {
                        if (response.body() != null) {
                            return new ApiCredentials(referenceId, response.body().getApiKey());
                        } else {
                            throw new RequestException(response.code(), response.message());
                        }
                    } else {
                        throw new RequestException(response.code(), response.message());
                    }
                });
    }

    Observable<Token> createToken(String type) {
        return createApiUser("")
                .flatMap(this::createApiKey)
                .flatMap(apiCredentials -> createToken(type, apiCredentials.getApiUser(), apiCredentials.getApiKey()));
    }

    Observable<Token> createToken(String providerCallbackHost, String type) {
        return createApiUser(providerCallbackHost)
                .flatMap(this::createApiKey)
                .flatMap(apiCredentials -> createToken(type, apiCredentials.getApiUser(), apiCredentials.getApiKey()));
    }

    Observable<Token> createToken(String type, String apiUser, String apiKey) {
        byte[] encodedBytes = Base64.getEncoder().encode((apiUser + ":" + apiKey).getBytes());
        String authorization = "Basic " + new String(encodedBytes);
        return RestClient
                .getService(getBaseUrl())
                .createToken(authorization, getSubscriptionKey(), type)
                .map(response -> {
                    if (response.code() == 200) {
                        Token token = response.body();
                        if (token != null) {
                            token.setApiKey(apiKey);
                            token.setApiUser(apiUser);
                        }
                        return token;
                    } else {
                        throw new RequestException(response.code(), response.message());
                    }
                });
    }

}