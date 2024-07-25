package com.outsystems.plugins.sslpinning;

import android.annotation.SuppressLint;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.apache.cordova.CordovaPreferences;

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

	@SuppressLint("DiscouragedApi")
	@Override
	protected void pluginInitialize() {
		super.pluginInitialize();

		CordovaPreferences preferences = this.preferences;
		if(preferences != null) {

			String fallbackUrl = preferences.getString("com.outsystems.experts.ssl.remote.fallback_url", null);
			boolean forceUrlFallbackUrl = Boolean.parseBoolean(preferences.getString("com.outsystems.experts.ssl.remote.force_use_fallback_url", "false"));

			Log.v("TAG", ">>> fallbackUrl :: "+fallbackUrl);
			Log.v("TAG", ">>> forceUrlFallbackUrl:: "+forceUrlFallbackUrl);

			pinningRemoteConfig = new PinningRemoteConfig(fallbackUrl, forceUrlFallbackUrl);
		} else {
			pinningRemoteConfig = new PinningRemoteConfig(null, false);
		}

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

		if(action.equals("checkCertificate")) {
			String urlString = args.getString(0);
			//request(urlString,callbackContext);
			requestCertificate(urlString,callbackContext);
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
		try {
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
		} catch (JSONException ex) {
			if(ex.getMessage() != null) {
				logger.logError(ex.getMessage(), "OSSSLPinning");
			}
			PluginResult pluginResultError = new PluginResult(PluginResult.Status.ERROR, ex.getMessage());
			callbackContext.sendPluginResult(pluginResultError);
			return null;
		}
	}

	private CertificatePinner showError() throws JSONException {
		JSONObject jsonErrorResponse = new JSONObject();
		PluginResult pluginResultError = new PluginResult(PluginResult.Status.ERROR, jsonErrorResponse);

		try {
			jsonErrorResponse.put("Code","1");
			jsonErrorResponse.put("Message","SSLPinning found a issue with the configured certificate for the url!");
			callbackContext.sendPluginResult(pluginResultError);
			return null;
		} catch (JSONException ex) {
			if(ex.getMessage() != null) {
				logger.logError(ex.getMessage(), "OSSSLPinning");
			}
			jsonErrorResponse.put("Code","1");
			jsonErrorResponse.put("Message",ex.getMessage());
			callbackContext.sendPluginResult(pluginResultError);
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
				future.completeExceptionally(new Exception(error != null ? error : "SSLPinning found a issue with the configured certificate"));
			}
		});

		return future;
	}

	private void requestCertificate(final String url, final CallbackContext callbackContext) {
		preferences.set("isSSLFirebaseRemoteFetch", true);

		try {
			makeRequest(url, callbackContext);
		} catch (Exception e) {
			sendError(callbackContext, "2", "SSLPinning found some problem with the request!");
		}
	}

	private void makeRequest(final String url, final CallbackContext callbackContext) {
		Request request = new Request.Builder().url(url).build();

		int timeout = 10000;

		OkHttpClient.Builder builder = getHttpClientBuilder().connectTimeout(timeout, TimeUnit.MILLISECONDS).readTimeout(timeout, TimeUnit.MILLISECONDS);
		OkHttpClient client = builder.build();

		Call call = client.newCall(request);

		call.enqueue(new Callback() {
			@Override
			public void onFailure(@NonNull Call call, @NonNull IOException e) {
				Log.v("TAG", ">>>>>>>>> ❌ >>>>>> Exception  :: "+e.getMessage());
				try {
					if (e instanceof SSLPeerUnverifiedException) {
						getHttpClientBuilder().build().certificatePinner().getPins().clear();
						fetchFallbackConfig(url, callbackContext);
					} else {
						logger.logError("Failed to build JSON response on failure: " + e.getMessage(), "OSSSLPinning", e);
					}
				} catch (Exception e1) {
					logger.logError("Failed to build JSON response on failure: " + e1.getMessage(), "OSSSLPinning", e1);
				}
			}

			@Override
			public void onResponse(@NonNull Call call, @NonNull Response response) {
				PluginResult pluginResultResponse = new PluginResult(PluginResult.Status.OK, true);
				callbackContext.sendPluginResult(pluginResultResponse);
			}
		});
	}

	private void fetchFallbackConfig(String url, CallbackContext callbackContext) {
		CordovaPreferences preferences = this.preferences;
		String fallbackUrl = preferences.getString("com.outsystems.experts.ssl.remote.fallback_url", null);
		if (fallbackUrl == null) {
			sendError(callbackContext, "1", "Failed to get the fallback URL, cannot be empty!");
		} else {
			new FetchFallbackConfigTask(url, callbackContext).execute(fallbackUrl);
		}
	}

	@SuppressLint("StaticFieldLeak")
	private class FetchFallbackConfigTask extends AsyncTask<String, Void, String> {
		private final String originalUrl;
		private final CallbackContext callbackContext;
		private Exception exception;

		public FetchFallbackConfigTask(String originalUrl, CallbackContext callbackContext) {
			this.originalUrl = originalUrl;
			this.callbackContext = callbackContext;
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
				return response.toString();
			} catch (Exception e) {
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
				sendError(callbackContext, "1", "Failed to fetch JSON from fallback API: " + exception.getMessage());
			} else if (result == null || result.isEmpty()) {
				sendError(callbackContext, "1", "JSON from fallback API is EMPTY!");
			} else {
				try {
					X509TrustManagerWrapper.buildCertificatePinningFromJson(result);
					request(originalUrl, callbackContext);
				} catch (Exception e) {
					sendError(callbackContext, "1", "Failed to parse and set pinning JSON from fallback: " + e.getMessage());
				}
			}
		}
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
				public void onFailure(@NonNull Call call, @NonNull IOException e) {
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
				public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {

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
			PluginResult pluginResultError = new PluginResult(PluginResult.Status.ERROR, jsonErrorResponse);
			callbackContext.sendPluginResult(pluginResultError);
		}
	}

	private void sendError(CallbackContext callbackContext, String code, String message) {
		JSONObject jsonErrorResponse = new JSONObject();
		try {
			jsonErrorResponse.put("Code", code);
			jsonErrorResponse.put("Message", message);
		} catch (JSONException e1) {
			logger.logError("Failed to build JSON response on failure: " + e1.getMessage(), "OSSSLPinning", e1);
		}
		PluginResult pluginResultError = new PluginResult(PluginResult.Status.ERROR, jsonErrorResponse);
		callbackContext.sendPluginResult(pluginResultError);
	}

	private OkHttpClient.Builder getHttpClientBuilder() {
		OkHttpClient.Builder clientBuilder = OkHttpClientWrapper.getInstance().getOkHttpClient().newBuilder();
		CertificatePinner certificatePinner = X509TrustManagerWrapper.getCertificatePinner();

		if(certificatePinner != null && !certificatePinner.getPins().isEmpty()) {
			clientBuilder.certificatePinner(certificatePinner);
		} else if (this.getCertificatePinner() != null && !this.getCertificatePinner().getPins().isEmpty()) {
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