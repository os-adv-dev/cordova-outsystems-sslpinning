package com.outsystems.plugins.sslpinning;

import android.os.Build;
import android.util.Log;

import androidx.annotation.Nullable;

import com.outsystems.plugins.oslogger.OSLogger;
import com.outsystems.plugins.oslogger.interfaces.Logger;
import com.outsystems.plugins.ossecurity.HttpClientCordovaPlugin;
import com.outsystems.plugins.ossecurity.enums.NetworkSetting;
import com.outsystems.plugins.ossecurity.interfaces.SSLSecurity;
import com.outsystems.plugins.sslpinning.pinning.OkHttpClientWrapper;
import com.outsystems.plugins.sslpinning.pinning.X509TrustManagerWrapper;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginManager;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.X509TrustManager;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.CertificatePinner;
import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class SSLPinningPlugin extends CordovaPlugin implements SSLSecurity {
	private static final String TAG = "OutSystems_SSL_PINNING";
	private static Logger logger = OSLogger.getInstance();

	private static final String CON_KEEP_ALIVE_PREF = "sslpinning-connection-keep-alive";

	// See https://square.github.io/okhttp/3.x/okhttp/okhttp3/ConnectionPool.html
	private static final int CON_MAX_IDLE_CONNECTIONS_DEFAULT = 5;
	private static final int CON_KEEP_ALIVE_DEFAULT = 300;

	private static CompletableFuture<CertificatePinner> certificatePinningFuture;
	private PinningRemoteConfig pinningRemoteConfig;

	private CallbackContext callbackContext;

	@Override
	protected void pluginInitialize() {
		super.pluginInitialize();

		pinningRemoteConfig = new PinningRemoteConfig();
		certificatePinningFuture = getCertificatePinningAsync();

		// Register Security implementation
		if(Build.VERSION.SDK_INT <= Build.VERSION_CODES.M){
			PluginManager pm = webView.getPluginManager();
			pm.postMessage(NetworkSetting.CERTIFICATE_PINNER.getId(), this);
		}
	}

	@Override
	public Object onMessage(String id, Object data) {
		try {
			if(Build.VERSION.SDK_INT <= Build.VERSION_CODES.M){
				String inspectorId = NetworkSetting.CERTIFICATE_PINNER.getId();
				if (!id.equalsIgnoreCase(inspectorId) && id.contains(inspectorId)) {
					((HttpClientCordovaPlugin) data).applyCertificatePinner(this);
				}
			}
		} catch (ClassCastException e) {
			logger.logError("Invalid type of data sent to predefined messages: " + e.getMessage(), TAG, e);
		}
		return super.onMessage(id, data);
	}

	@Override
	public boolean execute(String action, final JSONArray args, final CallbackContext callbackContext) throws JSONException {
		this.callbackContext = callbackContext;
		if (args == null || args.length() < 0) {
			logger.logDebug("Method execute invoked without arguments", "OSSSLPinning");
			return false;
		}

		if(action.equals("checkCertificate")){
			String urlString = args.getString(0);
			request(urlString,callbackContext);
			return true;
		}
		return false;
	}

	@Override
	public X509TrustManager getTrustManager() {
		return null;
	}

	@Override
	public SSLSocketFactory getSSLSocketFactory() {
		return null;
	}

	@Override
	public CertificatePinner getCertificatePinner() {
		if (getCertificatePinningFuture() != null) {
			Log.v("TAG", "✅ -- Getting from getCertificatePinningFuture ");
			if (getCertificatePinningFuture().getNow(null) != null) {
				return getCertificatePinningFuture().getNow(null);
			} else {
				return showError();
			}
		} else {
			return showError();
		}
	}

	private CertificatePinner showError() {
		JSONObject jsonErrorResponse = new JSONObject();
		try {
			jsonErrorResponse.put("Code","1");
			jsonErrorResponse.put("Message","SSLPinning found a issue with the configured certificate for the url!");
			PluginResult pluginResultError = new PluginResult(PluginResult.Status.ERROR, jsonErrorResponse);
			callbackContext.sendPluginResult(pluginResultError);
			return null;
		} catch (JSONException ex) {
			return null;
		}
	}

	@Override
	public OkHttpClient getOkHttpClient() {
		return OkHttpClientWrapper.getInstance().getOkHttpClient();
	}

	public static CompletableFuture<CertificatePinner> getCertificatePinningFuture() {
		return certificatePinningFuture;
	}

	public CompletableFuture<CertificatePinner> getCertificatePinningAsync() {
		CompletableFuture<CertificatePinner> future = new CompletableFuture<>();
		pinningRemoteConfig.getPinningJsonRemoteConfig(new RemoteConfigCallback() {
			@Override
			public void onConfigFetched(@Nullable String jsonObject) {
				if (jsonObject == null) {
					future.complete(null);
				} else {
					Log.v("TAG", "✅ -- Getting from onConfigFetched Remote: "+jsonObject);
					CertificatePinner remoteCertificate = X509TrustManagerWrapper.buildCertificatePinningFromJson(jsonObject);
					future.complete(remoteCertificate);
				}
			}

			@Override
			public void onError(@Nullable String error) {
				future.complete(null);
			}
		});

		return future;
	}

	private void request(final String url,final CallbackContext callbackContext) {
		preferences.set("isSSLFirebaseRemoteFetch", true);

		try{
			Request request = new Request.Builder().url(url).build();

			int timeout = 10000;

			OkHttpClient.Builder builder = getHttpClientBuilder().connectTimeout(timeout, TimeUnit.MILLISECONDS).readTimeout(timeout, TimeUnit.MILLISECONDS);
			OkHttpClient client = builder.build();

			Call call = client.newCall(request);

			call.enqueue(new Callback() {
				@Override
				public void onFailure(Call call, IOException e) {
					JSONObject jsonErrorResponse = new JSONObject();
					try {
						if (e instanceof SSLPeerUnverifiedException) {
							jsonErrorResponse.put("Code","1");
							jsonErrorResponse.put("Message","SSLPinning found a issue with the configured certificate for the url!");
						}else {
							jsonErrorResponse.put("Code","2");
							jsonErrorResponse.put("Message","SSLPinning found some problem with the request!");
						}
					} catch (JSONException e1) {
						logger.logError("Failed to build JSON response on failure: " + e1.getMessage(), "OSSSLPinning", e1);
					}
					PluginResult pluginResultError =
							new PluginResult(PluginResult.Status.ERROR, jsonErrorResponse);
					callbackContext.sendPluginResult(pluginResultError);
				}

				@Override
				public void onResponse(Call call, Response response) throws IOException {

					PluginResult pluginResultResponse = new PluginResult(PluginResult.Status.OK,true);
					callbackContext.sendPluginResult(pluginResultResponse);
				}
			});
		}catch (Exception e){
			JSONObject jsonErrorResponse = new JSONObject();
			try {
				jsonErrorResponse.put("Code","2");
				jsonErrorResponse.put("Message","SSLPinning found some problem with the request!");

			} catch (JSONException e1) {
				logger.logError("Failed to build JSON response on failure: " + e1.getMessage(), "OSSSLPinning", e1);
			}
			PluginResult pluginResultError =
					new PluginResult(PluginResult.Status.ERROR, jsonErrorResponse);
			callbackContext.sendPluginResult(pluginResultError);
		}
		
	}

	private OkHttpClient.Builder getHttpClientBuilder() {
		OkHttpClient.Builder clientBuilder = OkHttpClientWrapper.getInstance().getOkHttpClient().newBuilder();

		if (this.getCertificatePinner() != null) {
			clientBuilder.certificatePinner(this.getCertificatePinner());
		}

		try {
			// getInt can throw an exception if the user inserts a non-integer value. Don't let the exception bubble up and use default connection pool
			int keepAliveConnection = preferences.getInteger(CON_KEEP_ALIVE_PREF, CON_KEEP_ALIVE_DEFAULT);
			ConnectionPool cP = new ConnectionPool(CON_MAX_IDLE_CONNECTIONS_DEFAULT, keepAliveConnection, TimeUnit.SECONDS);
			clientBuilder.connectionPool(cP);
		} catch (Exception e) {
			logger.logError("Failed to get preference " + CON_KEEP_ALIVE_PREF + ": " + e.getMessage(), "OSSSLPinning", e);
		}
		return clientBuilder;
	}
}