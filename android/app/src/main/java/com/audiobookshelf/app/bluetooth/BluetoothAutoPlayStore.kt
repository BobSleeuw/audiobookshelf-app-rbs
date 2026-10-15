package com.audiobookshelf.app.bluetooth

import android.content.Context
import org.json.JSONObject

/** A Bluetooth device that was paired with or has connected to the phone. */
data class BluetoothAutoPlayDevice(val address: String, val name: String, val enabled: Boolean, val lastConnected: Long)

/**
 * Persists which Bluetooth devices should start playback when they connect, plus the devices seen connecting,
 * so they can be listed even after being unpaired. Kept in SharedPreferences so the broadcast receiver can
 * read it without the rest of the app running.
 */
object BluetoothAutoPlayStore {
  private const val PREFS = "abs_bluetooth_autoplay"
  private const val KEY_DEVICES = "devices"

  private fun read(ctx: Context): JSONObject {
    val raw = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_DEVICES, null)
    return try {
      if (raw == null) JSONObject() else JSONObject(raw)
    } catch (e: Exception) {
      JSONObject()
    }
  }

  private fun write(ctx: Context, obj: JSONObject) {
    ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_DEVICES, obj.toString()).apply()
  }

  @Synchronized
  fun getAll(ctx: Context): List<BluetoothAutoPlayDevice> {
    val obj = read(ctx)
    return obj.keys().asSequence().map { address ->
      val d = obj.getJSONObject(address)
      BluetoothAutoPlayDevice(address, d.optString("name", address), d.optBoolean("enabled", false), d.optLong("lastConnected", 0))
    }.toList()
  }

  @Synchronized
  fun isEnabled(ctx: Context, address: String): Boolean = read(ctx).optJSONObject(address)?.optBoolean("enabled", false) ?: false

  /** Remembers a device (keeps its toggle state) and updates its name / last connected time. */
  @Synchronized
  fun remember(ctx: Context, address: String, name: String?, connectedNow: Boolean) {
    val obj = read(ctx)
    val d = obj.optJSONObject(address) ?: JSONObject().put("enabled", false)
    if (!name.isNullOrBlank()) d.put("name", name) else if (!d.has("name")) d.put("name", address)
    if (connectedNow) d.put("lastConnected", System.currentTimeMillis())
    obj.put(address, d)
    write(ctx, obj)
  }

  @Synchronized
  fun setEnabled(ctx: Context, address: String, name: String?, enabled: Boolean) {
    remember(ctx, address, name, false)
    val obj = read(ctx)
    obj.getJSONObject(address).put("enabled", enabled)
    write(ctx, obj)
  }
}
