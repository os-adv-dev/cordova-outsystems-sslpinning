package com.outsystems.plugins.sslpinning.pinning;

import android.content.Context;
import android.content.res.AssetManager;

import com.outsystems.plugins.oslogger.OSLogger;
import com.outsystems.plugins.oslogger.interfaces.Logger;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;

import okhttp3.CertificatePinner;

public class X509TrustManagerWrapper {

  //TODO this will be removed later and injected through constructor
  private static Logger logger = OSLogger.getInstance();

  /**
   * SSL pinning with hash of certificates
   */
  public static CertificatePinner getPinningHash(Context context, String folder) {
    String json = loadJSONFromAsset(context, folder);

    if (json == null) return null;

    try {
      CertificatePinner.Builder builder = new CertificatePinner.Builder();
      JSONObject jsonObjectRoot = new JSONObject(json);

      if (jsonObjectRoot != null) {
        JSONArray jsonArrayHosts = jsonObjectRoot.getJSONArray("hosts");

        for (int i = 0; i < jsonArrayHosts.length(); i++) {
          JSONObject jsonObject = jsonArrayHosts.getJSONObject(i);
          String host = jsonObject.getString("host");
          JSONArray jsonArrayHashs = jsonObject.getJSONArray("hashes");
          for (int j = 0; j < jsonArrayHashs.length(); j++) {
            builder.add(host, (String) jsonArrayHashs.get(j));
          }
        }
      }

      return builder.build();
    } catch (JSONException e) {
      logger.logError("Failed to parse pinning JSON file: " + e.getMessage(), "OSSSLPinning", e);
      return null;
    }
  }

  private static String loadJSONFromAsset(Context context, String folder) {
    String json = null;
    try {
      AssetManager assetManager = context.getApplicationContext().getAssets();
      String[] fileList = assetManager.list("www/" + folder);
      String fileName = "";
      for (String name : fileList) {
        if (name.contains(".json")) {
          fileName = name;
          break;
        }
      }
      if (!fileName.equals("")) {
        InputStream is = context.getAssets().open("www/" + folder + "/" + fileName);
        int size = is.available();
        byte[] buffer = new byte[size];
        is.read(buffer);
        is.close();
        json = new String(buffer, "UTF-8");
      }
    } catch (IOException e) {
      logger.logError("Failed to fetch pinning file from assets: " + e.getMessage(), "OSSSLPinning", e);
      return null;
    }
    return json;
  }
}