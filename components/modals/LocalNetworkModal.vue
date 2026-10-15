<template>
  <modals-modal v-model="show" :width="'90%'" :max-width="'420px'" height="100%">
    <template #outer>
      <div class="absolute top-5 left-4 z-40">
        <p class="text-white text-2xl truncate">{{ $strings.HeaderLocalNetwork }}</p>
      </div>
    </template>
    <div class="w-full h-full overflow-hidden absolute top-0 left-0 flex items-center justify-center" @click="show = false">
      <div class="w-full rounded-lg bg-primary border border-white border-opacity-20 overflow-y-auto overflow-x-hidden p-4" style="max-height: 85vh" @click.stop>
        <p class="text-sm text-gray-300 mb-4">{{ $strings.MessageLocalNetworkDescription }}</p>

        <ui-text-input-with-label v-model="localAddress" :label="$strings.LabelLocalAddress" placeholder="http://192.168.1.10:13378" class="mb-1" />
        <p class="text-xs text-gray-400 mb-4">{{ $strings.LabelRemoteAddress }}: {{ remoteAddress }}</p>

        <ui-text-input-with-label v-model="ssidText" :label="$strings.LabelWifiNames" placeholder="HomeWifi, HomeWifi-5G" class="mb-1" />
        <p class="text-xs text-gray-400 mb-2">{{ $strings.MessageWifiNamesDescription }}</p>
        <ui-btn small class="mb-4" @click="addCurrentWifi">{{ $strings.ButtonUseCurrentWifi }}</ui-btn>

        <p v-if="testMessage" class="text-sm mb-3" :class="testOk ? 'text-success' : 'text-error'">{{ testMessage }}</p>
        <p v-if="error" class="text-sm text-error mb-3">{{ error }}</p>

        <div class="flex flex-wrap items-center gap-2">
          <ui-btn :disabled="!localAddress || processing" @click="test">{{ $strings.ButtonTestLocal }}</ui-btn>
          <ui-btn :disabled="processing" @click="testRemote">{{ $strings.ButtonTestNormal }}</ui-btn>
          <div class="flex-grow" />
          <ui-btn v-if="config && config.localAddress" color="error" :disabled="processing" @click="save(true)">{{ $strings.ButtonRemove }}</ui-btn>
          <ui-btn color="success" :disabled="processing" @click="save(false)">{{ $strings.ButtonSave }}</ui-btn>
        </div>
      </div>
    </div>
  </modals-modal>
</template>

<script>
import { AbsRbsSettings } from '@/plugins/capacitor/AbsRbsSettings'

export default {
  props: {
    value: Boolean,
    /** The saved server connection config being edited */
    config: Object
  },
  data() {
    return {
      localAddress: '',
      ssidText: '',
      processing: false,
      testMessage: '',
      testOk: false,
      error: ''
    }
  },
  watch: {
    show(val) {
      if (val) this.init()
    }
  },
  computed: {
    show: {
      get() {
        return this.value
      },
      set(val) {
        this.$emit('input', val)
      }
    },
    remoteAddress() {
      return this.config?.remoteAddress || this.config?.address || ''
    }
  },
  methods: {
    init() {
      this.localAddress = this.config?.localAddress || ''
      this.ssidText = (this.config?.localSsids || []).join(', ')
      this.testMessage = ''
      this.error = ''
      this.processing = false
    },
    parseSsids() {
      return this.ssidText
        .split(',')
        .map((s) => s.trim())
        .filter((s) => s)
    },
    async addCurrentWifi() {
      this.error = ''
      try {
        const perm = await AbsRbsSettings.requestLocationPermission()
        if (!perm.granted) {
          this.error = this.$strings.MessageLocationPermissionNeeded
          return
        }
        const { ssid } = await AbsRbsSettings.getWifiSsid()
        if (!ssid) {
          this.error = this.$strings.MessageWifiNameUnavailable
          return
        }
        const ssids = this.parseSsids()
        if (!ssids.includes(ssid)) ssids.push(ssid)
        this.ssidText = ssids.join(', ')
      } catch (e) {
        console.error('[LocalNetworkModal] Failed to get Wi-Fi name', e)
        this.error = this.$strings.MessageWifiNameUnavailable
      }
    },
    async test() {
      const address = this.$localNetwork.normalizeAddress(this.localAddress)
      if (!address) return
      this.processing = true
      this.testMessage = ''
      const result = await this.$localNetwork.testConnection(address)
      this.testOk = result.ok
      this.testMessage = `${this.$strings.LabelLocalAddress}: ${this.$localNetwork.describeTest(result, this.$strings)}`
      this.processing = false
    },
    async testRemote() {
      if (!this.remoteAddress) return
      this.processing = true
      this.testMessage = ''
      const result = await this.$localNetwork.testConnection(this.remoteAddress, this.config?.customHeaders)
      this.testOk = result.ok
      this.testMessage = `${this.$strings.LabelRemoteAddress}: ${this.$localNetwork.describeTest(result, this.$strings)}`
      this.processing = false
    },
    /** @param {boolean} remove - Remove the local address */
    async save(remove) {
      if (!this.config) return
      this.processing = true
      this.error = ''
      try {
        const remote = this.remoteAddress
        const payload = { id: this.config.id, remoteAddress: remote }
        if (remove || !this.localAddress.trim()) {
          // Back to using the remote address only
          Object.assign(payload, { localAddress: '', localSsids: [], address: remote })
        } else {
          Object.assign(payload, { localAddress: this.$localNetwork.normalizeAddress(this.localAddress), localSsids: this.parseSsids() })
        }
        let updated = await this.$db.updateServerAddresses(payload)

        // Switch right away when the local address is usable on this network
        const active = await this.$localNetwork.resolveAddress(updated)
        if (active !== updated.address) {
          updated = await this.$db.updateServerAddresses({ id: updated.id, address: active })
        }

        // Keep the stored copies in sync
        this.$store.commit('setDeviceData', await this.$db.getDeviceData())
        const current = this.$store.state.user.serverConnectionConfig
        if (current?.id === updated.id) {
          this.$store.commit('user/setServerConnectionConfig', { ...current, address: updated.address, remoteAddress: updated.remoteAddress, localAddress: updated.localAddress, localSsids: updated.localSsids })
        }
        this.$emit('saved', updated)
        this.show = false
      } catch (e) {
        console.error('[LocalNetworkModal] Failed to save', e)
        this.error = this.$strings.ToastFailedToUpdate
      } finally {
        this.processing = false
      }
    }
  }
}
</script>
