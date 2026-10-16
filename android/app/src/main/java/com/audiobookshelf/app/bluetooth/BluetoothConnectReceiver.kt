package com.audiobookshelf.app.bluetooth

import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import com.audiobookshelf.app.settings.RbsSettingsStore
import android.util.Log
import com.audiobookshelf.app.player.PlayerNotificationService

/** Starts playback of the last played book when a Bluetooth device enabled for auto play connects. */
class BluetoothConnectReceiver : BroadcastReceiver() {
  private val tag = "BluetoothConnectReceiver"

  override fun onReceive(context: Context, intent: Intent) {
    if (intent.action != BluetoothDevice.ACTION_ACL_CONNECTED) return

    val device: BluetoothDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
    } else {
      @Suppress("DEPRECATION") intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
    }
    val address = device?.address ?: return
    val name = try {
      device.name
    } catch (e: SecurityException) {
      null
    }

    BluetoothAutoPlayStore.remember(context, address, name, true)
    if (!RbsSettingsStore.bluetoothAutoPlayEnabled(context)) return
    if (!BluetoothAutoPlayStore.isEnabled(context, address)) return

    // ACL connect can fire repeatedly while a device (re)connects
    val now = System.currentTimeMillis()
    if (now - lastTriggerTime < 10_000 && lastTriggerAddress == address) return
    lastTriggerTime = now
    lastTriggerAddress = address

    Log.i(tag, "Auto play device connected: $name ($address)")
    try {
      val serviceIntent = Intent(context, PlayerNotificationService::class.java).apply {
        action = PlayerNotificationService.ACTION_BLUETOOTH_AUTOPLAY
      }
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        context.startForegroundService(serviceIntent)
      } else {
        context.startService(serviceIntent)
      }
    } catch (e: Exception) {
      // Android may refuse to start a foreground service from the background in some states.
      // A media play key is allowed from the background and reaches the media session / media button receiver.
      Log.e(tag, "Failed to start player service for auto play (${e.message}), sending media play key")
      try {
        Handler(Looper.getMainLooper()).postDelayed({
          val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
          audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY))
          audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PLAY))
        }, RbsSettingsStore.bluetoothDelaySeconds(context) * 1000L)
      } catch (e2: Exception) {
        Log.e(tag, "Failed to send media play key: ${e2.message}")
      }
    }
  }

  companion object {
    private var lastTriggerTime = 0L
    private var lastTriggerAddress = ""
  }
}
