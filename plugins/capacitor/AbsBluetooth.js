import { registerPlugin, WebPlugin } from '@capacitor/core'

/**
 * @typedef {Object} BluetoothAutoPlayDevice
 * @property {string} address
 * @property {string} name
 * @property {boolean} enabled - Start playback when this device connects
 * @property {number} lastConnected - Epoch ms, 0 when never seen connecting
 */

class AbsBluetoothWeb extends WebPlugin {
  async hasBluetoothPermission() {
    return { granted: false }
  }

  async requestBluetoothPermission() {
    return { granted: false }
  }

  async getDevices() {
    return { granted: false, devices: [] }
  }

  async setDeviceEnabled() {}
}

const AbsBluetooth = registerPlugin('AbsBluetooth', {
  web: () => new AbsBluetoothWeb()
})

export { AbsBluetooth }
