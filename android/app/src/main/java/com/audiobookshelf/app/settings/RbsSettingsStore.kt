package com.audiobookshelf.app.settings

import android.content.Context

/**
 * Settings added by ABS-RBS. Stored in SharedPreferences (not the device settings database) so the Bluetooth
 * receiver and the player service can read them without the web layer, and old saved data stays compatible.
 */
object RbsSettingsStore {
  private const val PREFS = "abs_rbs_settings"
  const val KEY_BLUETOOTH_DELAY = "bluetoothDelaySeconds"
  const val KEY_VOLUME_BOOST = "volumeBoostDb"
  const val KEY_ANDROID_AUTO_BOOST = "androidAutoVolumeBoostDb"

  const val DEFAULT_BLUETOOTH_DELAY_SECONDS = 5
  const val MAX_BLUETOOTH_DELAY_SECONDS = 60
  const val MAX_BOOST_DB = 15

  private fun prefs(ctx: Context) = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

  /** Seconds to wait after a Bluetooth device connects before playback starts. */
  fun bluetoothDelaySeconds(ctx: Context): Int =
    prefs(ctx).getInt(KEY_BLUETOOTH_DELAY, DEFAULT_BLUETOOTH_DELAY_SECONDS).coerceIn(0, MAX_BLUETOOTH_DELAY_SECONDS)

  /** Volume boost in dB used for normal playback. */
  fun volumeBoostDb(ctx: Context): Int = prefs(ctx).getInt(KEY_VOLUME_BOOST, 0).coerceIn(0, MAX_BOOST_DB)

  /** Volume boost in dB used while connected to Android Auto. */
  fun androidAutoBoostDb(ctx: Context): Int = prefs(ctx).getInt(KEY_ANDROID_AUTO_BOOST, 0).coerceIn(0, MAX_BOOST_DB)

  fun setInt(ctx: Context, key: String, value: Int) {
    prefs(ctx).edit().putInt(key, value).apply()
  }
}
