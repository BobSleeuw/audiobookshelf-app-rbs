package com.audiobookshelf.app.plugins

import android.Manifest
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.audiobookshelf.app.bluetooth.BluetoothAutoPlayDevice
import com.audiobookshelf.app.bluetooth.BluetoothAutoPlayStore
import com.getcapacitor.JSArray
import com.getcapacitor.JSObject
import com.getcapacitor.Plugin
import com.getcapacitor.PluginCall
import com.getcapacitor.PluginMethod
import com.getcapacitor.annotation.CapacitorPlugin
import com.getcapacitor.annotation.Permission
import com.getcapacitor.annotation.PermissionCallback

/** Lets the web UI list Bluetooth devices and choose which ones start playback when they connect. */
@CapacitorPlugin(
  name = "AbsBluetooth",
  permissions = [Permission(strings = [Manifest.permission.BLUETOOTH_CONNECT], alias = "bluetooth")]
)
class AbsBluetooth : Plugin() {
  private fun hasPermission(): Boolean {
    // BLUETOOTH_CONNECT is a runtime permission from Android 12, earlier versions use install-time permissions
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
    return ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
  }

  @PluginMethod
  fun hasBluetoothPermission(call: PluginCall) {
    call.resolve(JSObject().put("granted", hasPermission()))
  }

  @PluginMethod
  fun requestBluetoothPermission(call: PluginCall) {
    if (hasPermission()) {
      call.resolve(JSObject().put("granted", true))
    } else {
      requestPermissionForAlias("bluetooth", call, "bluetoothPermissionCallback")
    }
  }

  @PermissionCallback
  private fun bluetoothPermissionCallback(call: PluginCall) {
    call.resolve(JSObject().put("granted", hasPermission()))
  }

  /** Paired devices merged with devices seen connecting, with their auto play toggle state. */
  @PluginMethod
  fun getDevices(call: PluginCall) {
    val ctx: Context = context
    if (!hasPermission()) {
      call.resolve(JSObject().put("granted", false).put("devices", JSArray()))
      return
    }
    try {
      val adapter = (ctx.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter
      adapter?.bondedDevices?.forEach { d ->
        BluetoothAutoPlayStore.remember(ctx, d.address, d.name, false)
      }
      val devices = JSArray()
      BluetoothAutoPlayStore.getAll(ctx)
        .sortedWith(compareByDescending<BluetoothAutoPlayDevice> { it.enabled }.thenBy { it.name.lowercase() })
        .forEach {
          devices.put(JSObject().put("address", it.address).put("name", it.name).put("enabled", it.enabled).put("lastConnected", it.lastConnected))
        }
      call.resolve(JSObject().put("granted", true).put("devices", devices))
    } catch (e: SecurityException) {
      call.resolve(JSObject().put("granted", false).put("devices", JSArray()))
    }
  }

  @PluginMethod
  fun setDeviceEnabled(call: PluginCall) {
    val address = call.getString("address")
    if (address == null) {
      call.reject("address required")
      return
    }
    val enabled = call.getBoolean("enabled", false) ?: false
    BluetoothAutoPlayStore.setEnabled(context, address, call.getString("name"), enabled)
    call.resolve()
  }
}
