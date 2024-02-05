package com.outsystems.plugins.sslpinning;

import android.util.Log;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;

public class PinningRemoteConfig {

    FirebaseRemoteConfig firebaseRemoteConfig = FirebaseRemoteConfig.getInstance();
    private static final String SSL_PINNING_CONFIG = "ssl_pinning_config";
    private static final String IS_SSL_ENABLED = "is_enabled_ssl_pinning";

    public PinningRemoteConfig() {
        FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings.Builder()
            .setMinimumFetchIntervalInSeconds(0)
            .build();
        firebaseRemoteConfig.setConfigSettingsAsync(configSettings);
    }

    public void getPinningJsonRemoteConfig(final RemoteConfigCallback callback) {
        firebaseRemoteConfig.fetchAndActivate().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                boolean isRemoteSslPinningEnabled = firebaseRemoteConfig.getBoolean(IS_SSL_ENABLED);
                Log.v("TAG", "-- ✅ IS_SSL_ENABLED : " + isRemoteSslPinningEnabled);
                if (isRemoteSslPinningEnabled) {
                    String jsonString = firebaseRemoteConfig.getString(SSL_PINNING_CONFIG);
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
