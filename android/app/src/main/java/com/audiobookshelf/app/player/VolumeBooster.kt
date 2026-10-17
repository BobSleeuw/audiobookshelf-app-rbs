package com.audiobookshelf.app.player

import android.content.Context
import android.database.ContentObserver
import android.media.audiofx.LoudnessEnhancer
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.audiobookshelf.app.settings.RbsSettingsStore

/**
 * Boosts the playback volume above the normal maximum with a [LoudnessEnhancer] on the player's audio session.
 * The gain depends on whether the phone is connected to Android Auto (separate setting).
 */
object VolumeBooster {
  private const val TAG = "VolumeBooster"
  private val CAR_CONNECTION_URI: Uri = Uri.parse("content://androidx.car.app.connection")
  private const val CAR_CONNECTION_COLUMN = "CarConnectionState"

  private var appContext: Context? = null
  private var enhancer: LoudnessEnhancer? = null
  private var observer: ContentObserver? = null

  /** True while the phone is connected to Android Auto (projection) or a native car OS. */
  @Volatile
  var carConnected = false
    private set

  /**
   * Set when Android Auto (or its simulator) browses this app's media session. Only used when the Android Auto
   * connection provider can't be read, so the Android Auto boost still applies.
   */
  @Volatile
  var projectionBrowsing = false

  /** Selectable boost levels (dB) for the Android Auto menu and button; each has an icon ic_boost_<index>. */
  val LEVELS = (0..20 step 2).toList()

  /** Called after a boost setting or the car connection changed, so UIs (Android Auto) can refresh. */
  @Volatile
  var onChanged: (() -> Unit)? = null

  /** Index of the closest level to [db]. */
  fun levelIndex(db: Int): Int = LEVELS.indices.minByOrNull { Math.abs(LEVELS[it] - db) } ?: 0

  /** Sets the Android Auto boost and applies it. */
  fun setAndroidAutoBoost(ctx: Context, db: Int) {
    RbsSettingsStore.setInt(ctx, RbsSettingsStore.KEY_ANDROID_AUTO_BOOST, db.coerceIn(0, RbsSettingsStore.MAX_BOOST_DB))
    apply()
  }

  /** The Android Auto button: each tap raises the boost by 2 dB, after the maximum it goes back to off. */
  fun stepAndroidAutoBoost(ctx: Context) {
    val level = RbsSettingsStore.androidAutoBoostDb(ctx)
    val next = (level / 2 + 1) * 2
    setAndroidAutoBoost(ctx, if (next > RbsSettingsStore.MAX_BOOST_DB) 0 else next)
  }

  /** Gain in dB currently requested for the active connection type. */
  fun currentBoostDb(ctx: Context): Int {
    if (!RbsSettingsStore.volumeBoostEnabled(ctx)) return 0
    return if (carConnected) RbsSettingsStore.androidAutoBoostDb(ctx) else RbsSettingsStore.volumeBoostDb(ctx)
  }

  /** Attaches to the audio session of the player. Call again whenever the session id changes. */
  @Synchronized
  fun attach(ctx: Context, audioSessionId: Int) {
    appContext = ctx.applicationContext
    releaseEnhancer()
    if (audioSessionId <= 0) return
    try {
      enhancer = LoudnessEnhancer(audioSessionId)
    } catch (e: Exception) {
      Log.e(TAG, "Failed to create LoudnessEnhancer for session $audioSessionId: ${e.message}")
      enhancer = null
    }
    apply()
  }

  /** Applies the current settings; call after a setting or the car connection changed. */
  @Synchronized
  fun apply(notify: Boolean = true) {
    val ctx = appContext ?: return
    refreshCarConnection()
    val enhancer = enhancer
    if (enhancer == null) {
      Log.w(TAG, "No audio enhancer attached yet (car=$carConnected)")
      if (notify) onChanged?.invoke()
      return
    }
    val db = currentBoostDb(ctx)
    try {
      enhancer.setTargetGain(db * 100) // millibels
      enhancer.enabled = db > 0
      Log.i(TAG, "Boost ${db}dB applied (car=$carConnected, enabled=${enhancer.enabled})")
    } catch (e: Exception) {
      Log.e(TAG, "Failed to apply boost: ${e.message}")
    }
    if (notify) onChanged?.invoke()
  }

  /** Re-reads the car connection and applies the gain; call when playback starts. */
  fun refresh() {
    val before = carConnected
    apply(notify = false)
    if (before != carConnected) onChanged?.invoke()
  }

  /** Starts watching the Android Auto connection state (provided by the Android Auto app). */
  @Synchronized
  fun startMonitoring(ctx: Context) {
    appContext = ctx.applicationContext
    refreshCarConnection()
    if (observer != null) return
    observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
      override fun onChange(selfChange: Boolean) {
        refreshCarConnection()
        apply()
      }
    }
    try {
      ctx.applicationContext.contentResolver.registerContentObserver(CAR_CONNECTION_URI, true, observer!!)
    } catch (e: Exception) {
      Log.e(TAG, "Failed to observe car connection: ${e.message}")
    }
  }

  @Synchronized
  fun release() {
    releaseEnhancer()
    observer?.let {
      try {
        appContext?.contentResolver?.unregisterContentObserver(it)
      } catch (e: Exception) {
        Log.e(TAG, "Failed to unregister observer: ${e.message}")
      }
    }
    observer = null
  }

  private fun releaseEnhancer() {
    try {
      enhancer?.release()
    } catch (e: Exception) {
      Log.e(TAG, "Failed to release enhancer: ${e.message}")
    }
    enhancer = null
  }

  private fun refreshCarConnection() {
    val ctx = appContext ?: return
    var connected = false
    var answered = false
    try {
      ctx.contentResolver.query(CAR_CONNECTION_URI, arrayOf(CAR_CONNECTION_COLUMN), null, null, null)?.use { cursor ->
        val index = cursor.getColumnIndex(CAR_CONNECTION_COLUMN)
        if (index >= 0 && cursor.moveToNext()) {
          // 0 = not connected, 1 = native car OS, 2 = Android Auto projection
          connected = cursor.getInt(index) > 0
          answered = true
        }
      }
    } catch (e: Exception) {
      // Android Auto not installed or provider unavailable
    }
    // Without an answer from Android Auto, use the fact that it is browsing this app
    if (!answered) connected = projectionBrowsing
    if (connected != carConnected) Log.i(TAG, "Car connection changed: $connected (provider answered=$answered)")
    carConnected = connected
  }
}
