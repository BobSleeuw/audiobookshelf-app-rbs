import { registerPlugin, WebPlugin } from '@capacitor/core'

/**
 * @typedef {Object} RbsSettings
 * @property {boolean} bluetoothAutoPlayEnabled - Master switch; the per device toggles are kept when it is off
 * @property {boolean} volumeBoostEnabled - Master switch for both boost levels
 * @property {number} bluetoothDelaySeconds - Wait after a Bluetooth device connects before playback starts
 * @property {number} volumeBoostDb - Volume boost for normal playback
 * @property {number} androidAutoVolumeBoostDb - Volume boost while connected to Android Auto
 * @property {number} maxBoostDb
 * @property {number} maxBluetoothDelaySeconds
 * @property {boolean} ignoringBatteryOptimizations - Needed to start playback from the background
 * @property {boolean} carConnected
 */

class AbsRbsSettingsWeb extends WebPlugin {
  async getSettings() {
    return {
      progressMode: 'CHAPTER',
      androidAutoProgressMode: 'CHAPTER',
      bluetoothAutoPlayEnabled: true,
      bluetoothStartWhenClosed: true,
      bluetoothDelaySeconds: 5,
      volumeBoostEnabled: false,
      volumeBoostDb: 0,
      androidAutoVolumeBoostDb: 0,
      maxBoostDb: 20,
      maxBluetoothDelaySeconds: 60,
      ignoringBatteryOptimizations: true,
      carConnected: false
    }
  }

  async setSetting() {
    return this.getSettings()
  }

  async requestIgnoreBatteryOptimizations() {}

  async getWifiSsid() {
    return { permissionGranted: false, ssid: null }
  }

  async requestLocationPermission() {
    return { granted: false }
  }
}

const AbsRbsSettings = registerPlugin('AbsRbsSettings', {
  web: () => new AbsRbsSettingsWeb()
})

export { AbsRbsSettings }
