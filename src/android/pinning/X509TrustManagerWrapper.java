package com.outsystems.plugins.sslpinning.pinning;

import com.outsystems.plugins.oslogger.OSLogger;
import com.outsystems.plugins.oslogger.interfaces.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import okhttp3.CertificatePinner;

public class X509TrustManagerWrapper {

  private static Logger logger = OSLogger.getInstance();

  public static CertificatePinner buildCertificatePinningFromJson(String json) {
    if (json == null) return null;

    CertificatePinner.Builder builder = new CertificatePinner.Builder();
    try {
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
}