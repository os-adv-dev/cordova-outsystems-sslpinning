package com.outsystems.plugins.sslpinning;

public interface RemoteConfigCallback {
    void onConfigFetched(String jsonObject);
    void onError(String error);
}