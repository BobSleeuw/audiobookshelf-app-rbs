package com.audiobookshelf.app.plugins

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import androidx.core.content.ContextCompat
import com.getcapacitor.annotation.Permission
import com.getcapacitor.annotation.PermissionCallback
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import com.audiobookshelf.app.player.VolumeBooster
import com.audiobookshelf.app.settings.RbsSettingsStore
import com.getcapacitor.JSObject
import com.getcapacitor.Plugin
import com.getcapacitor.PluginCall
import com.getcapacitor.PluginMethod
import com.getcapacitor.annotation.CapacitorPlugin

/** Web access to the ABS-RBS specific settings (Bluetooth delay, volume boost, background start permission). */
@CapacitorPlugin(
  name = "AbsRbsSettings",
  permissions = [Permission(strings = [Manifest.permission.ACCESS_FINE_LOCATION], alias = "location")]
)
class AbsRbsSettings : Plugin() {
  private fun hasLocationPermission(): Boolean {
    return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
  }

  /** Name of the connected Wi-Fi network, or null when not on Wi-Fi / unknown (needs the location permission). */
  @Suppress("DEPRECATION")
  private fun currentSsid(): String? {
    if (!hasLocationPermission()) return null
    val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager ?: return null
    val ssid = wifiManager.connectionInfo?.ssid ?: return null
    if (ssid == WifiManager.UNKNOWN_SSID || ssid == "<unknown ssid>") return null
    return ssid.trim('"')
  }

  @PluginMethod
  fun getWifiSsid(call: PluginCall) {
    call.resolve(JSObject().put("permissionGranted", hasLocationPermission()).put("ssid", currentSsid()))
  }

  @PluginMethod
  fun requestLocationPermission(call: PluginCall) {
    if (hasLocationPermission()) {
      call.resolve(JSObject().put("granted", true))
    } else {
      requestPermissionForAlias("location", call, "locationPermissionCallback")
    }
  }

  @PermissionCallback
  private fun locationPermissionCallback(call: PluginCall) {
    call.resolve(JSObject().put("granted", hasLocationPermission()))
  }

  private fun isIgnoringBatteryOptimizations(): Boolean {
    val pm = context.getSystemService(PowerManager::class.java)
    return pm?.isIgnoringBatteryOptimizations(context.packageName) ?: false
  }

  private fun settingsObject(): JSObject {
    return JSObject()
      .put("bluetoothDelaySeconds", RbsSettingsStore.bluetoothDelaySeconds(context))
      .put("volumeBoostDb", RbsSettingsStore.volumeBoostDb(context))
      .put("androidAutoVolumeBoostDb", RbsSettingsStore.androidAutoBoostDb(context))
      .put("volumeBoostEnabled", RbsSettingsStore.volumeBoostEnabled(context))
      .put("bluetoothAutoPlayEnabled", RbsSettingsStore.bluetoothAutoPlayEnabled(context))
      .put("bluetoothStartWhenClosed", RbsSettingsStore.bluetoothStartWhenClosed(context))
      .put("progressMode", RbsSettingsStore.progressMode(context))
      .put("androidAutoQueueMode", RbsSettingsStore.androidAutoQueueMode(context))
      .put("androidAutoProgressMode", RbsSettingsStore.androidAutoProgressMode(context))
      .put("maxBoostDb", RbsSettingsStore.MAX_BOOST_DB)
      .put("maxBluetoothDelaySeconds", RbsSettingsStore.MAX_BLUETOOTH_DELAY_SECONDS)
      .put("ignoringBatteryOptimizations", isIgnoringBatteryOptimizations())
      .put("carConnected", VolumeBooster.carConnected)
  }

  @PluginMethod
  fun getSettings(call: PluginCall) {
    call.resolve(settingsObject())
  }

  /** Sets one of: bluetoothDelaySeconds, volumeBoostDb, androidAutoVolumeBoostDb (numbers), volumeBoostEnabled, bluetoothAutoPlayEnabled (booleans) */
  @PluginMethod
  fun setSetting(call: PluginCall) {
    val key = call.getString("key")
    if (key == RbsSettingsStore.KEY_BOOST_ENABLED || key == RbsSettingsStore.KEY_BLUETOOTH_ENABLED || key == RbsSettingsStore.KEY_BLUETOOTH_START_WHEN_CLOSED) {
      val flag = call.getBoolean("value")
      if (flag == null) {
        call.reject("value required")
        return
      }
      RbsSettingsStore.setBoolean(context, key, flag)
      if (key == RbsSettingsStore.KEY_BOOST_ENABLED) VolumeBooster.apply()
      call.resolve(settingsObject())
      return
    }
    if (key == RbsSettingsStore.KEY_ANDROID_AUTO_QUEUE_MODE) {
      val mode = call.getString("value")
      if (mode != RbsSettingsStore.QUEUE_CHAPTERS && mode != RbsSettingsStore.QUEUE_FILES) {
        call.reject("invalid queue mode")
        return
      }
      RbsSettingsStore.setString(context, key, mode)
      VolumeBooster.onChanged?.invoke() // refreshes the queue Android Auto shows
      call.resolve(settingsObject())
      return
    }
    if (key == RbsSettingsStore.KEY_PROGRESS_MODE || key == RbsSettingsStore.KEY_ANDROID_AUTO_PROGRESS_MODE) {
      val mode = call.getString("value")
      if (mode != RbsSettingsStore.PROGRESS_CHAPTER && mode != RbsSettingsStore.PROGRESS_BOOK) {
        call.reject("invalid progress mode")
        return
      }
      RbsSettingsStore.setString(context, key, mode)
      VolumeBooster.onChanged?.invoke() // refreshes what the notification and Android Auto show
      call.resolve(settingsObject())
      return
    }
    val value = call.getInt("value")
    if (key == null || value == null) {
      call.reject("key and value required")
      return
    }
    when (key) {
      RbsSettingsStore.KEY_BLUETOOTH_DELAY -> RbsSettingsStore.setInt(context, key, value.coerceIn(0, RbsSettingsStore.MAX_BLUETOOTH_DELAY_SECONDS))
      RbsSettingsStore.KEY_VOLUME_BOOST, RbsSettingsStore.KEY_ANDROID_AUTO_BOOST -> {
        RbsSettingsStore.setInt(context, key, value.coerceIn(0, RbsSettingsStore.MAX_BOOST_DB))
        VolumeBooster.apply()
      }
      else -> {
        call.reject("unknown setting $key")
        return
      }
    }
    call.resolve(settingsObject())
  }

  /**
   * Android lets apps that are excluded from battery optimization start playback from the background,
   * which Bluetooth auto play needs when the app is closed.
   */
  @PluginMethod
  fun requestIgnoreBatteryOptimizations(call: PluginCall) {
    try {
      val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
        data = Uri.parse("package:${context.packageName}")
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
      context.startActivity(intent)
      call.resolve()
    } catch (e: Exception) {
      try {
        context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        call.resolve()
      } catch (e2: Exception) {
        call.reject(e2.message ?: "Could not open battery settings")
      }
    }
  }
}
