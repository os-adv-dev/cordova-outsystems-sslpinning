package com.outsystems.plugins.sslpinning;

import android.util.Log;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;

import com.outsystems.plugins.oscache.OSCache;
import com.outsystems.plugins.oscache.cache.interfaces.CacheEngine;
import com.outsystems.plugins.oslogger.OSLogger;
import com.outsystems.plugins.oslogger.interfaces.Logger;
import com.outsystems.plugins.ossecurity.HttpClientCordovaPlugin;
import com.outsystems.plugins.ossecurity.enums.NetworkSetting;
import com.outsystems.plugins.ossecurity.interfaces.NetworkInspector;
import com.outsystems.plugins.ossecurity.interfaces.SSLSecurity;
import com.outsystems.plugins.sslpinning.pinning.OkHttpClientWrapper;
import com.outsystems.plugins.sslpinning.types.RequestEvents;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.ICordovaCookieManager;
import org.apache.cordova.PluginManager;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLHandshakeException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.ConnectionPool;
import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class NativeXMLHttpRequestPlugin extends HttpClientCordovaPlugin {
  private static final String TAG = "OS_SSL_PINNING";

  private static final String ACTION_SEND = "send";
  private static final String ACTION_ABORT = "abort";

  // User can customize keep alive time for idle connections through preferences
  private static final String CON_KEEP_ALIVE_PREF = "sslpinning-connection-keep-alive";

  // See https://square.github.io/okhttp/3.x/okhttp/okhttp3/ConnectionPool.html
  private static final int CON_MAX_IDLE_CONNECTIONS_DEFAULT = 5;
  private static final int CON_KEEP_ALIVE_DEFAULT = 300;

  private static final String HEADER_CONTENT_TYPE = "Content-Type";

  private Map<Long, RequestEvents> requestsEvents;
  private NetworkInspector networkInspector;
  private SSLSecurity sslSecurity;
  private CookieHandler cookieHandler;

  //TODO this will be removed later and injected through constructor
  private Logger logger = OSLogger.getInstance();

  @Override
  public void initialize(CordovaInterface cordova, CordovaWebView webView) {
    super.initialize(cordova, webView);

    requestsEvents = new HashMap<Long, RequestEvents>();
    cookieHandler = new CookieHandler(this.webView.getCookieManager());
    webView.getPluginManager().postMessage(NetworkSetting.NETWORK_INSPECTOR.getId() + getClass().getName(), this);
    webView.getPluginManager().postMessage(NetworkSetting.CERTIFICATE_PINNER.getId() + getClass().getName(), this);
  }

  @Override
  public boolean execute(String action, final JSONArray args, final CallbackContext callbackContext) throws JSONException {

    if (args == null || args.length() < 0) {
      this.logger.logDebug("Method execute invoked without arguments", "OSSSLPinning");
      return false;
    }

    if (action.equals(ACTION_SEND)) {
      try {
        String urlString = args.getString(0);
        this.logger.logDebug("URL: " + urlString, "OSSSLPinning");
        String method = args.getString(1);
        JSONObject headers = args.getJSONObject(2);
        HashMap<String, String> headersMap = this.getStringMapFromJSONObject(headers);
        boolean async = args.getBoolean(4);
        int timeout = 10000;
        if (args.get(5) != null && !args.get(5).toString().equals("null") && args.getInt(5) >= 0) {
          timeout = args.getInt(5);
        }

        Long instanceId = args.getLong(6);

        if (method.equals("post")) {
          String params = args.getString(3);
          requestPost(urlString, params, headersMap, timeout, callbackContext, instanceId);
        } else if (method.equals("get")) {
          requestResource(urlString, callbackContext, instanceId, headersMap, timeout);
        }
      } catch (JSONException e) {
        this.logger.logError("Error processing JSON arguments: " + e.getMessage(), "OSSSLPinning", e);
        callbackContext.error("Error processing JSON arguments");
      }
      return true;
    } else if (action.equals(ACTION_ABORT)) {
      Long instanceId = args.getLong(0);
      abortRequest(instanceId, callbackContext);
      return true;
    }
    return false;
  }

  public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

  private HashMap<String, Object> getMapFromJSONObject(JSONObject object) throws JSONException {
    HashMap<String, Object> map = new HashMap<String, Object>();
    Iterator<?> i = object.keys();

    while (i.hasNext()) {
      String key = (String) i.next();
      map.put(key, object.get(key));
    }
    return map;
  }

  private HashMap<String, String> getStringMapFromJSONObject(JSONObject object)
          throws JSONException {
    HashMap<String, String> map = new HashMap<String, String>();
    Iterator<?> i = object.keys();

    while (i.hasNext()) {
      String key = (String) i.next();
      map.put(key, object.getString(key));
    }
    return map;
  }

  private void sendLoadEndEvent(CallbackContext callbackContext) {
    JSONObject jsonObject = new JSONObject();
    try {
      jsonObject.put("event", "loadend");
      jsonObject.put("callbackid", callbackContext.getCallbackId());
      PluginResult pluginResult = new PluginResult(PluginResult.Status.OK, jsonObject);
      pluginResult.setKeepCallback(false);

      callbackContext.sendPluginResult(pluginResult);
    } catch (JSONException e) {
      this.logger.logError("Failed to build content of plugin result: " + e.getMessage(), "OSSSLPinning", e);
    }
  }

  private void abortRequest(Long instanceId, CallbackContext callbackContext) {
    RequestEvents requestEvents = requestsEvents.get(instanceId);
    if (requestEvents != null && !requestEvents.getCall().isCanceled()) {
      requestEvents.getCall().cancel();
      this.logger.logDebug("Abort request with ID: " + instanceId, "OSSSLPinning");

      callbackContext.success();
      removeRequestFromMap(instanceId);
    } else {
      callbackContext.error("Request is not running");
    }
  }

  private void requestGet(String url, HashMap<String, String> headers, int timeout,
                          final CallbackContext callbackContext, Long instanceId) {
    Request request =
            new Request.Builder().url(url).headers(buildHeaders(headers).build()).get().build();

    request(url, request, timeout, callbackContext, instanceId);
  }

  private void requestPost(final String url, String data, HashMap<String, String> headers,
                           int timeout, final CallbackContext callbackContext, Long instanceId) {
    RequestBody requestBody;
    try {
      requestBody = generateRequestBody(data, headers);
    } catch (IllegalArgumentException e) {
        this.logger.logError(HEADER_CONTENT_TYPE + " header has an invalid value", "OSSSLPinning", e);
        callbackContext.error(HEADER_CONTENT_TYPE + " header has an invalid value");
        return;
    }

    Request request = new Request.Builder().url(url).headers(buildHeaders(headers).build()).post(requestBody).build();
    request(url, request, timeout, callbackContext, instanceId);
  }

  /**
   * Generates the HTTP POST request body, setting its proper content-type.
   */
  private RequestBody generateRequestBody(String body, HashMap<String, String> headers) throws IllegalArgumentException{
    if (body != null) {
      // Get Content-Type
      final String contentType = getCaseInsensitiveHeaderValue(HEADER_CONTENT_TYPE, headers);

      // Get request body and validate if provided Content-Type has a valid value
      final MediaType mediaType = contentType != null ? MediaType.get(contentType) : null;
      return RequestBody.create(mediaType, body);
    } else {
      // Generate empty body if none is provided
      return RequestBody.create(null, new byte[0]);
    }
  }

  private static String getCaseInsensitiveHeaderValue(String header, HashMap<String, String> headers){
    for (String key : headers.keySet()) {
      if(header.equalsIgnoreCase(key)){
        return headers.get(key);
      }
    }
    return null;
  }

  /**
   * Gives priority to load resource from Cache Plugin and only if fails, load from network
   *
   * @param url
   * @param callbackContext
   * @param instanceId
   */
  private void requestResource(final String url, final CallbackContext callbackContext, final Long instanceId, final HashMap<String, String> headers, final int timeout) {
    cordova.getThreadPool().execute(new Runnable() {
      @Override
      public void run() {
        PluginManager pm = webView.getPluginManager();
        OSCache osCachePlugin = (OSCache) pm.getPlugin(OSCache.CORDOVA_SERVICE_NAME);
        CacheEngine cacheEngine = osCachePlugin.getCacheEngine();

        JSONObject jsonObject = cacheEngine.getJSONResourceFromCache(url);
        if (jsonObject != null && jsonObject.length() > 0) {
          try {
            PluginResult pluginResultResponse = new PluginResult(PluginResult.Status.OK,
                    buildJSONResponse("load", callbackContext.getCallbackId(), jsonObject));
            pluginResultResponse.setKeepCallback(true);
            callbackContext.sendPluginResult(pluginResultResponse);

            sendLoadEndEvent(callbackContext);
            removeRequestFromMap(instanceId);
          } catch (Exception e) {
            logger.logError("Failed to request resource: " + e.getMessage(), "OSSSLPinning", e);
          }
        } else {
          requestGet(url, headers, timeout, callbackContext, instanceId);
        }
      }
    });
  }

  /**
   * @param request
   * @param callbackContext
   */
  private void request(final String url, Request request, int timeout,
                       final CallbackContext callbackContext, final Long instanceId) {

    OkHttpClient.Builder builder = getHttpClientBuilder().connectTimeout(timeout, TimeUnit.MILLISECONDS).readTimeout(timeout, TimeUnit.MILLISECONDS);

    OkHttpClient client = builder.build();

    Call call = client.newCall(request);
    addRequestToMap(instanceId, call);

    call.enqueue(new Callback() {
      @Override
      public void onFailure(Call call, IOException e) {
        JSONObject jsonErrorResponse = new JSONObject();
        try {
          if (!(e instanceof ConnectException)) {
            logger.logError("The pinning request failed with IOException: " + e.getMessage(), "OSSSLPinning", e);
          }
          logger.logDebug("Request call is cancelled: " + call.isCanceled(), "OSSSLPinning");
          logger.logDebug("Request call is executed: " + call.isExecuted(), "OSSSLPinning");
          if (call.isCanceled()) {
            jsonErrorResponse.put("event", "abort");
          } else if (e instanceof SocketTimeoutException) {
            logger.logDebug("SocketTimeoutException on request", "OSSSLPinning");
            jsonErrorResponse.put("event", "timeout");
          } else if (e instanceof SSLHandshakeException) {
            logger.logDebug("SSLHandshakeException on request", "OSSSLPinning");
            jsonErrorResponse.put("event", "error");
            JSONObject jsonError = new JSONObject();
            jsonError.put("statusText", e.getMessage());
            jsonErrorResponse.put("arg", jsonError);
          } else {
            logger.logDebug("UnexpectedException on request", "OSSSLPinning");
            jsonErrorResponse.put("event", "error");
            JSONObject jsonError = new JSONObject();
            jsonError.put("statusText", e.getMessage());
            jsonErrorResponse.put("arg", jsonError);
          }
          jsonErrorResponse.put("callbackid", callbackContext.getCallbackId());
        } catch (JSONException e1) {
          logger.logError("Failed to build JSON response on failure: " + e1.getMessage(), "OSSSLPinning", e1);
        }

        logger.logDebug("Request response: " + jsonErrorResponse.toString(), "OSSSLPinning");

        PluginResult pluginResultError =
                new PluginResult(PluginResult.Status.ERROR, jsonErrorResponse);
        pluginResultError.setKeepCallback(true);
        callbackContext.sendPluginResult(pluginResultError);

        sendLoadEndEvent(callbackContext);

        removeRequestFromMap(instanceId);
      }

      @Override
      public void onResponse(Call call, Response response) throws IOException {
        if (response.code() != 200) {
          logger.logDebug("Response code " + response.code() + " with request URL: " + response.request().url().toString(), "OSSSLPinning");
        }

        JSONObject jsonObject = new JSONObject();
        try {
          jsonObject.put("statusCode", response.code());
          jsonObject.put("data", response.body().string());
          jsonObject.put("statusText", response.message());
          jsonObject.put("headers", response.headers().toString());

          if (response.request().method().equals("POST")) {
            CookieManager cookieManager = CookieManager.getInstance();
            cookieManager.setAcceptCookie(true);
            for (int i = 0; i < response.headers().size(); i++) {
              cookieManager.setCookie(url, response.headers().value(i));
              CookieSyncManager.getInstance().sync();
            }
            cookieManager.setAcceptCookie(true);
          }
          PluginResult pluginResultResponse = new PluginResult(PluginResult.Status.OK,
                  buildJSONResponse("load", callbackContext.getCallbackId(), jsonObject));
          pluginResultResponse.setKeepCallback(true);
          callbackContext.sendPluginResult(pluginResultResponse);

          sendLoadEndEvent(callbackContext);
          removeRequestFromMap(instanceId);

          logger.logDebug("Body response: " + response.body().toString(), "OSSSLPinning");
        } catch (JSONException e) {
          logger.logError("Failed to build JSON response on response: " + e.getMessage(), "OSSSLPinning", e);
        }
      }
    });
  }

  private Headers.Builder buildHeaders(HashMap<String, String> headers) {
    Headers.Builder builder = new Headers.Builder();
    for (String key : headers.keySet()) {
      builder.add(key, headers.get(key));
    }

    return builder;
  }

  /**
   * Method to add a request running to hash map
   */
  private void addRequestToMap(Long instanceId, Call call) {
    try {
      requestsEvents.put(instanceId, new RequestEvents(call));
    } catch (UnsupportedOperationException e) {
      logger.logError("UnsupportedOperationException while adding request to map: " + e.getMessage(), "OSSSLPinning", e);
    } catch (ClassCastException e) {
      logger.logError("ClassCastException while adding request to map: " + e.getMessage(), "OSSSLPinning", e);
    } catch (IllegalArgumentException e) {
      logger.logError("IllegalArgumentException while adding request to map: " + e.getMessage(), "OSSSLPinning", e);
    } catch (NullPointerException e) {
      logger.logError("NullPointerException while adding request to map: " + e.getMessage(), "OSSSLPinning", e);
    }
  }

  /**
   * Method to remove request that finished or was cancelled from map
   */
  private void removeRequestFromMap(Long instanceId) {
    try {
      requestsEvents.remove(instanceId);
    } catch (UnsupportedOperationException e) {
      logger.logError("UnsupportedOperationException while removing request from map: " + e.getMessage(), "OSSSLPinning", e);
    }
  }

  /**
   * Method to build the general response for each event that will be fired during the request
   *
   * @param event - Event can be Progress, load, loadend...
   */
  private JSONObject buildJSONResponse(String event, String callBackId, JSONObject body) {
    try {
      JSONObject jsonObject = new JSONObject();
      jsonObject.put("event", event);
      jsonObject.put("arg", body);
      jsonObject.put("callbackid", callBackId);
      return jsonObject;
    } catch (JSONException e) {
      logger.logError("Failed to build JSON response: " + e.getMessage(), "OSSSLPinning", e);
    }
    return null;
  }

  @Override
  public void applyNetworkInspector(NetworkInspector data) {
    networkInspector = data;
  }

  @Override
  public void applyCertificatePinner(SSLSecurity data) {
    sslSecurity = data;
  }

  private OkHttpClient.Builder getHttpClientBuilder() {
    OkHttpClient.Builder clientBuilder = OkHttpClientWrapper.getInstance().getOkHttpClient().newBuilder();
    clientBuilder.addInterceptor(new Interceptor() {
      @Override
      public Response intercept(Chain chain) throws IOException {
        final Request original = chain.request();

        String cookies = CookieManager.getInstance().getCookie(original.url().toString());
        if (cookies != null && cookies.length() > 0) {
          final Request authorized = original.newBuilder()
                  .addHeader("Cookie", cookies)
                  .build();

          return chain.proceed(authorized);
        }
        return chain.proceed(original);
      }
    });

    if (sslSecurity != null && sslSecurity.getCertificatePinner() != null) {
      clientBuilder.certificatePinner(sslSecurity.getCertificatePinner());
    }

    if (networkInspector != null && networkInspector.getNetworkInterceptor() != null) {
      clientBuilder.addInterceptor(networkInspector.getNetworkInterceptor());
    }

    clientBuilder.cookieJar(cookieHandler);

    try {
      // getInt can throw an exception if the user inserts a non-integer value. Don't let the exception bubble up and use default connection pool
      int keepAliveConnection = preferences.getInteger(CON_KEEP_ALIVE_PREF, CON_KEEP_ALIVE_DEFAULT);
      ConnectionPool cP = new ConnectionPool(CON_MAX_IDLE_CONNECTIONS_DEFAULT, keepAliveConnection, TimeUnit.SECONDS);
      clientBuilder.connectionPool(cP);
    } catch (Exception e) {
      this.logger.logError("Failed to get preference " + CON_KEEP_ALIVE_PREF + ": " + e.getMessage(), "OSSSLPinning", e);
    }
    return clientBuilder;
  }

  static class CookieHandler implements CookieJar {

    private final ICordovaCookieManager cordovaCookieManager;

    public CookieHandler(ICordovaCookieManager cordovaCookieManager) {
      this.cordovaCookieManager = cordovaCookieManager;
    }

    @Override
    public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
      final String urlString = url.toString();

      for (Cookie cookie : cookies) {
        this.cordovaCookieManager.setCookie(urlString, cookie.toString());
      }

      if (!cookies.isEmpty()) {
        this.cordovaCookieManager.flush();
      }
    }

    @Override
    public List<Cookie> loadForRequest(HttpUrl url) {
      final String urlString = url.toString();
      final String cookiesString = this.cordovaCookieManager.getCookie(urlString);

      if (cookiesString != null && !cookiesString.isEmpty()) {
        // We can split on the ';' char as the cookie manager only returns cookies
        // that match the url and haven't expired, so the cookie attributes aren't included
        final String[] cookieHeaders = cookiesString.split(";");
        final List<Cookie> cookies = new ArrayList<Cookie>(cookieHeaders.length);

        for (String header : cookieHeaders) {
          final Cookie cookie = Cookie.parse(url, header);

          if (cookie != null) {
            cookies.add(cookie);
          } else {
            // TODO: Use OutSystems Logger
            Log.w(this.getClass().getName(), "Could not parse cookie with header " + header);
          }
        }

        return cookies;
      }

      return Collections.emptyList();
    }
  }

}
