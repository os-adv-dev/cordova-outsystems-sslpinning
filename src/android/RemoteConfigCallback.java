package com.outsystems.plugins.sslpinning;

public interface RemoteConfigCallback {
    /**
     * This function receive the json SSL Pinning from Remote
     * @param jsonObject the JSON
     */
    void onConfigFetched(String jsonObject);

    /**
     * This function receive the error when try to get the JSON from Remote
     * @param error the error message
     */
    void onError(String error);
}