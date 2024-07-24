package com.outsystems.plugins.sslpinning;

import android.util.Pair;
import android.webkit.MimeTypeMap;
import android.webkit.WebResourceResponse;

import androidx.annotation.NonNull;

import com.outsystems.plugins.oscache.cache.helpers.MimeTypesHelper;
import com.outsystems.plugins.oslogger.OSLogger;
import com.outsystems.plugins.oslogger.interfaces.Logger;
import com.outsystems.plugins.sslpinning.pinning.OkHttpClientWrapper;
import com.outsystems.plugins.sslpinning.pinning.X509TrustManagerWrapper;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.CertificatePinner;
import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class AddPinningWebClient {

    private final Logger logger = OSLogger.getInstance();

    public WebResourceResponse getSSLUrlValidation(String url, Map<String, String> headers) {
        CompletableFuture<Pair<Boolean, String>> sslPinningFuture = new CompletableFuture<>();
        requestSSLPinning(url, new SSLErrorCallback() {
            @Override
            public void onError(String code, String message) {
                logger.logError("Failed to parse pinning JSON file: "+message, "OSSSLPinning");
                sslPinningFuture.complete(new Pair<>(false, message));
            }

            @Override
            public void onSuccess() {
                sslPinningFuture.complete(new Pair<>(true, "success"));
            }
        });

        try {
            if (!sslPinningFuture.get().first) {
                String extension = MimeTypeMap.getFileExtensionFromUrl(url);
                String mimeType = MimeTypesHelper.getInstance().getMimeType(extension);
                return new WebResourceResponse(mimeType, "UTF-8", 525, "SSLPinning found some problem with the request "+ sslPinningFuture.get().second, headers, null);
            }
        } catch (InterruptedException | ExecutionException e) {
            logger.logError("Failed to parse pinning ExecutionException: " + e.getMessage(), "OSSSLPinning");
            e.printStackTrace();
            return new WebResourceResponse("text/plain", "UTF-8", 525, "SSLPinning execution error: " + e.getMessage(), headers, null);
        }
        return null;
    }

    private void requestSSLPinning(final String url, final SSLErrorCallback callback) {
        Request request = new Request.Builder().url(url).build();

        int timeout = 10000;

        OkHttpClient.Builder builder = getHttpClientBuilder().connectTimeout(timeout, TimeUnit.MILLISECONDS).readTimeout(timeout, TimeUnit.MILLISECONDS);
        OkHttpClient client = builder.build();

        Call call = client.newCall(request);

        call.enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                logger.logError("Failure to requestSSLPinning : " + e.getMessage(), "OSSSLPinning");
                callback.onError("525", "SSLPinning found some problem with the request: "+e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                if(response.isSuccessful()) {
                    callback.onSuccess();
                } else {
                    String message = response.message();
                    callback.onError(String.valueOf(response.code()), "SSLPinning found some problem with the request: "+message);
                }
            }
        });
    }

    public interface SSLErrorCallback {
        void onError(String code, String message);
        void onSuccess();
    }

    private OkHttpClient.Builder getHttpClientBuilder() {
        OkHttpClient.Builder clientBuilder = OkHttpClientWrapper.getInstance().getOkHttpClient().newBuilder();
        CertificatePinner remoteCertificate = X509TrustManagerWrapper.getCertificatePinner();

        if (remoteCertificate != null && !remoteCertificate.getPins().isEmpty()) {
            clientBuilder.certificatePinner(remoteCertificate);
        }

        try {
            // getInt can throw an exception if the user inserts a non-integer value. Don't let the exception bubble up and use default connection pool
            int keepAliveConnection = 300;
            ConnectionPool cP = new ConnectionPool(5, keepAliveConnection, TimeUnit.SECONDS);
            clientBuilder.connectionPool(cP);
        } catch (Exception e) {
            logger.logError("Failed to get preference " + "sslpinning-connection-keep-alive" + ": " + e.getMessage(), "OSSSLPinning");
        }
        return clientBuilder;
    }
}