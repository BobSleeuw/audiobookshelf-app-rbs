<template>
  <div class="w-full h-full px-4 py-8 overflow-y-auto">
    <p class="uppercase text-xs font-semibold text-fg-muted mb-2">{{ $strings.HeaderBluetoothAutoPlay }}</p>
    <p class="text-sm text-fg-muted mb-4">{{ $strings.MessageBluetoothAutoPlayDescription }}</p>

    <!-- Delay before playing -->
    <div class="py-3 mb-2">
      <div class="flex items-center">
        <p class="flex-grow">{{ $strings.LabelBluetoothPlayDelay }}</p>
        <p class="text-sm tabular-nums">{{ delaySeconds }} s</p>
      </div>
      <input v-model.number="delaySeconds" type="range" min="0" :max="maxDelaySeconds" step="1" class="w-full" @change="saveDelay" />
      <p class="text-xs text-fg-muted mt-1">{{ $strings.MessageBluetoothPlayDelayDescription }}</p>
    </div>

    <!-- Background start -->
    <div class="py-3 mb-4">
      <p class="font-semibold mb-1">{{ $strings.HeaderBluetoothBackgroundStart }}</p>
      <p v-if="ignoringBatteryOptimizations" class="text-sm text-success">{{ $strings.MessageBluetoothBackgroundStartAllowed }}</p>
      <template v-else>
        <p class="text-sm text-fg-muted mb-3">{{ $strings.MessageBluetoothBackgroundStartDescription }}</p>
        <ui-btn small @click="allowBackgroundStart">{{ $strings.ButtonAllowBackgroundStart }}</ui-btn>
      </template>
    </div>

    <p class="uppercase text-xs font-semibold text-fg-muted mb-2">{{ $strings.HeaderBluetoothDevices }}</p>

    <div v-if="loading" class="py-8 flex justify-center">
      <ui-loading-indicator />
    </div>

    <div v-else-if="!granted" class="py-4">
      <p class="mb-4">{{ $strings.MessageBluetoothPermissionRequired }}</p>
      <ui-btn @click="requestPermission">{{ $strings.ButtonGrantPermission }}</ui-btn>
    </div>

    <template v-else>
      <p v-if="!devices.length" class="py-4 text-center text-fg-muted">{{ $strings.MessageNoBluetoothDevices }}</p>
      <div v-for="device in devices" :key="device.address" class="flex items-center py-3 border-b border-white border-opacity-10">
        <div class="flex-grow min-w-0 pr-4">
          <p class="truncate">{{ device.name }}</p>
          <p class="text-xs text-fg-muted truncate">{{ lastConnectedText(device) }}</p>
        </div>
        <div class="w-12 flex justify-center" @click.stop="toggle(device)">
          <ui-toggle-switch :value="device.enabled" class="pointer-events-none" />
        </div>
      </div>
      <div class="pt-6 flex justify-center">
        <ui-btn small @click="loadDevices">{{ $strings.ButtonRefresh }}</ui-btn>
      </div>
    </template>
  </div>
</template>

<script>
import { AbsBluetooth } from '@/plugins/capacitor/AbsBluetooth'
import { AbsRbsSettings } from '@/plugins/capacitor/AbsRbsSettings'

export default {
  data() {
    return {
      loading: true,
      granted: false,
      devices: [],
      delaySeconds: 5,
      maxDelaySeconds: 60,
      ignoringBatteryOptimizations: true
    }
  },
  methods: {
    async loadRbsSettings() {
      try {
        const settings = await AbsRbsSettings.getSettings()
        this.delaySeconds = settings.bluetoothDelaySeconds
        this.maxDelaySeconds = settings.maxBluetoothDelaySeconds || 60
        this.ignoringBatteryOptimizations = !!settings.ignoringBatteryOptimizations
      } catch (error) {
        console.error('[bluetooth] Failed to load settings', error)
      }
    },
    async saveDelay() {
      try {
        await AbsRbsSettings.setSetting({ key: 'bluetoothDelaySeconds', value: this.delaySeconds })
      } catch (error) {
        console.error('[bluetooth] Failed to save delay', error)
        this.$toast.error(this.$strings.ToastFailedToUpdate)
      }
    },
    async allowBackgroundStart() {
      try {
        await AbsRbsSettings.requestIgnoreBatteryOptimizations()
      } catch (error) {
        console.error('[bluetooth] Failed to open battery settings', error)
      }
    },
    lastConnectedText(device) {
      if (!device.lastConnected) return this.$strings.LabelNeverConnected
      return new Date(device.lastConnected).toLocaleString()
    },
    async requestPermission() {
      const res = await AbsBluetooth.requestBluetoothPermission()
      this.granted = !!res.granted
      if (this.granted) await this.loadDevices()
    },
    async loadDevices() {
      this.loading = true
      try {
        const res = await AbsBluetooth.getDevices()
        this.granted = !!res.granted
        this.devices = res.devices || []
      } catch (error) {
        console.error('[bluetooth] Failed to load devices', error)
        this.$toast.error(this.$strings.ToastFailedToLoad)
      } finally {
        this.loading = false
      }
    },
    async toggle(device) {
      const enabled = !device.enabled
      try {
        await AbsBluetooth.setDeviceEnabled({ address: device.address, name: device.name, enabled })
        device.enabled = enabled
      } catch (error) {
        console.error('[bluetooth] Failed to update device', error)
        this.$toast.error(this.$strings.ToastFailedToUpdate)
      }
    }
  },
  async mounted() {
    // Refresh when returning from the system battery settings dialog
    this.onVisible = () => {
      if (document.visibilityState === 'visible') this.loadRbsSettings()
    }
    document.addEventListener('visibilitychange', this.onVisible)
    await this.loadRbsSettings()
    const perm = await AbsBluetooth.hasBluetoothPermission()
    this.granted = !!perm.granted
    if (this.granted) await this.loadDevices()
    else this.loading = false
  },
  beforeDestroy() {
    document.removeEventListener('visibilitychange', this.onVisible)
  }
}
</script>
