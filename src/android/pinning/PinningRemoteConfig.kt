package com.outsystems.plugins.sslpinning

import android.content.Context
import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import org.json.JSONException

interface RemoteConfigCallback {
    fun onConfigFetched(jsonObject: String?)
    fun onError(error: String?)
}

class PinningRemoteConfig {

    private val firebaseRemoteConfig: FirebaseRemoteConfig = FirebaseRemoteConfig.getInstance()

    init {
        val configSettings = FirebaseRemoteConfigSettings.Builder().setMinimumFetchIntervalInSeconds(0).build()
        firebaseRemoteConfig.setConfigSettingsAsync(configSettings)
    }

    fun getPinningJsonRemoteConfig(callback: RemoteConfigCallback) {
        firebaseRemoteConfig.fetchAndActivate().addOnCompleteListener { task: Task<Boolean?> ->
            if (task.isSuccessful) {
                val isRemoteSslPinningEnabled = firebaseRemoteConfig.getBoolean(IS_SSL_ENABLED)
                Log.v("TAG", "-- ✅ IS_SSL_ENABLED : $isRemoteSslPinningEnabled")
                if (isRemoteSslPinningEnabled) {
                    val jsonString = firebaseRemoteConfig.getString(SSL_PINNING_CONFIG)
                    try {
                        callback.onConfigFetched(jsonString)
                    } catch (e: JSONException) {
                        callback.onError(null)
                    } catch (e: Exception) {
                        callback.onError(null)
                    }
                } else {
                    callback.onError(null)
                }
            } else {
                callback.onError(null)
            }
        }
    }

    companion object {
        private const val SSL_PINNING_CONFIG = "ssl_pinning_config"
        private const val IS_SSL_ENABLED = "is_enabled_ssl_pinning"
    }
}