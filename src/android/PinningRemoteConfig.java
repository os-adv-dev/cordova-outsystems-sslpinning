package com.outsystems.plugins.sslpinning;

import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;

public class PinningRemoteConfig {

    FirebaseRemoteConfig firebaseRemoteConfig = FirebaseRemoteConfig.getInstance();
    private static final String SSL_PINNING_CONFIG = "ssl_pinning_config";

    public PinningRemoteConfig() {
        FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings.Builder()
            .setMinimumFetchIntervalInSeconds(0)
            .build();
        firebaseRemoteConfig.setConfigSettingsAsync(configSettings);
    }

    public void getPinningJsonRemoteConfig(final RemoteConfigCallback callback) {
        firebaseRemoteConfig.fetchAndActivate().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                String jsonString = firebaseRemoteConfig.getString(SSL_PINNING_CONFIG);
                if (!jsonString.isEmpty()) {
                    try {
                        callback.onConfigFetched(jsonString);
                    } catch (Exception e) {
                        callback.onError(null);
                    }
                } else {
                    callback.onError(null);
                }
            } else {
                callback.onError(null);
            }
        });
    }
}
