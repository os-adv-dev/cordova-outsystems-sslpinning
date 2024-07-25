package com.outsystems.plugins.sslpinning;

import android.annotation.SuppressLint;
import android.os.AsyncTask;
import android.util.Log;

import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;
import com.outsystems.plugins.oslogger.OSLogger;
import com.outsystems.plugins.oslogger.interfaces.Logger;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class PinningRemoteConfig {

    FirebaseRemoteConfig firebaseRemoteConfig = FirebaseRemoteConfig.getInstance();
    private static final String SSL_PINNING_CONFIG = "ssl_pinning_config";
    private static final String TAG = "OutSystems_SSL_PINNING";
    private static final Logger logger = OSLogger.getInstance();
    private String serverPinningUrl = null;
    private boolean forceUrlFallbackUrl = false;

    public PinningRemoteConfig(String fallbackUrl, boolean forceUrlFallbackUrl) {
        // Get the fallback Server URL to download the SSL Pinning JSON
        this.serverPinningUrl = fallbackUrl;
        this.forceUrlFallbackUrl = forceUrlFallbackUrl;

        FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(0)
                .build();
        firebaseRemoteConfig.setConfigSettingsAsync(configSettings);
    }

    public void getPinningJsonRemoteConfig(final RemoteConfigCallback callback) {
        if (forceUrlFallbackUrl) {
            fetchFallbackConfig(callback);
        } else {
            firebaseRemoteConfig.fetchAndActivate().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    String jsonString = firebaseRemoteConfig.getString(SSL_PINNING_CONFIG);
                    if (!jsonString.isEmpty()) {
                        try {
                            callback.onConfigFetched(jsonString);
                        } catch (Exception e) {
                            Log.v(TAG, "PinningRemoteConfig: " + e.getMessage());
                            logger.logError("Failed to fetch JSON from Firebase Remote Config: " + e.getMessage(), "OSSSLPinning");
                            fetchFallbackConfig(callback);
                        }
                    } else {
                        logger.logError("JSON from Firebase Remote Config is EMPTY!", "OSSSLPinning");
                        fetchFallbackConfig(callback);
                    }
                } else {
                    fetchFallbackConfig(callback);
                }
            }).addOnFailureListener(task -> {
                logger.logError("Failed to fetch JSON from Firebase Remote Config: " + task.getMessage(), "OSSSLPinning");
                fetchFallbackConfig(callback);
            });
        }
    }

    private void fetchFallbackConfig(RemoteConfigCallback callback) {
        if (serverPinningUrl != null && !serverPinningUrl.isEmpty()) {
            logger.logDebug("fetchFallbackConfig calling the API:  "+serverPinningUrl  , "OSSSLPinning");
            new FetchFallbackConfigTask(callback).execute(serverPinningUrl);
        } else {
            callback.onError("Error to retrieve the Json SSL Pinning Firebase Remote Config or URL Fallback");
        }
    }

    @SuppressLint("StaticFieldLeak")
    private class FetchFallbackConfigTask extends AsyncTask<String, Void, String> {
        private final RemoteConfigCallback callback;
        private Exception exception;

        public FetchFallbackConfigTask(RemoteConfigCallback callback) {
            this.callback = callback;
        }

        @Override
        protected String doInBackground(String... urls) {
            HttpURLConnection urlConnection = null;
            try {
                URL url = new URL(urls[0]);
                urlConnection = (HttpURLConnection) url.openConnection();
                BufferedReader in = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();
                String responseJson = response.toString();
                logger.logDebug("fetchFallbackConfig JSON download success the API:  "+serverPinningUrl+" - json response: "+responseJson  , "OSSSLPinning");
                return responseJson;
            } catch (Exception e) {
                logger.logDebug("fetchFallbackConfig JSON download error the API:  "+serverPinningUrl+" - json response: "+e.getMessage(), "OSSSLPinning");
                exception = e;
                return null;
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
            }
        }

        @Override
        protected void onPostExecute(String result) {
            if (exception != null) {
                logger.logError("Failed to fetch JSON from fallback API: " + exception, "OSSSLPinning", exception);
                callback.onError("Failed to fetch JSON from fallback API: " + exception);
            } else if (result == null || result.isEmpty()) {
                logger.logError("Failed to fetch JSON from fallback API: " + serverPinningUrl, "OSSSLPinning");
                callback.onError("JSON from fallback API is EMPTY!");
            } else {
                logger.logDebug("onPostExecute JSON success result: error the API:  "+serverPinningUrl+" - json response: "+result, "OSSSLPinning");
                callback.onConfigFetched(result);
            }
        }
    }
}