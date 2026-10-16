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
  const val KEY_ANDROID_AUTO_BOOST_ACTIVE = "androidAutoBoostActive"
  const val KEY_PROGRESS_MODE = "progressMode"
  const val KEY_ANDROID_AUTO_PROGRESS_MODE = "androidAutoProgressMode"
  const val PROGRESS_CHAPTER = "CHAPTER"
  const val PROGRESS_BOOK = "BOOK"
  const val KEY_BOOST_ENABLED = "volumeBoostEnabled"
  const val KEY_BLUETOOTH_ENABLED = "bluetoothAutoPlayEnabled"
  const val KEY_BLUETOOTH_START_WHEN_CLOSED = "bluetoothStartWhenClosed"

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

  /** Master switch for volume boost (normal and Android Auto). Defaults to on when a boost level was already set. */
  fun volumeBoostEnabled(ctx: Context): Boolean {
    val p = prefs(ctx)
    return if (p.contains(KEY_BOOST_ENABLED)) p.getBoolean(KEY_BOOST_ENABLED, false) else (volumeBoostDb(ctx) > 0 || androidAutoBoostDb(ctx) > 0)
  }

  /** Master switch for Bluetooth auto play; the per device toggles are kept when it is off. */
  fun bluetoothAutoPlayEnabled(ctx: Context): Boolean = prefs(ctx).getBoolean(KEY_BLUETOOTH_ENABLED, true)

  /** The Android Auto boost button switches the boost on and off; the level itself is set in the app. */
  fun androidAutoBoostActive(ctx: Context): Boolean = prefs(ctx).getBoolean(KEY_ANDROID_AUTO_BOOST_ACTIVE, true)

  /** What the notification shows as progress: the current chapter or the whole book. */
  fun progressMode(ctx: Context): String = prefs(ctx).getString(KEY_PROGRESS_MODE, PROGRESS_CHAPTER) ?: PROGRESS_CHAPTER

  /** What Android Auto shows as progress: the current chapter or the whole book. */
  fun androidAutoProgressMode(ctx: Context): String = prefs(ctx).getString(KEY_ANDROID_AUTO_PROGRESS_MODE, PROGRESS_CHAPTER) ?: PROGRESS_CHAPTER

  fun setString(ctx: Context, key: String, value: String) {
    prefs(ctx).edit().putString(key, value).apply()
  }

  /** When off, Bluetooth auto play only starts while the player is already running (not from a closed app). */
  fun bluetoothStartWhenClosed(ctx: Context): Boolean = prefs(ctx).getBoolean(KEY_BLUETOOTH_START_WHEN_CLOSED, true)

  fun setBoolean(ctx: Context, key: String, value: Boolean) {
    prefs(ctx).edit().putBoolean(key, value).apply()
  }

  fun setInt(ctx: Context, key: String, value: Int) {
    prefs(ctx).edit().putInt(key, value).apply()
  }
}
