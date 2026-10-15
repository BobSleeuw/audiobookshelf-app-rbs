import { CapacitorHttp } from '@capacitor/core'
import { Network } from '@capacitor/network'
import { AbsRbsSettings } from './capacitor/AbsRbsSettings'

/**
 * Picks which address of a server to use: the local address while on the local network, otherwise the remote one.
 * The local address is used when the device is on Wi-Fi/LAN, the Wi-Fi name matches (when names are configured and
 * readable) and the local server answers a quick ping.
 */
export default function ({ store }, inject) {
  const PROBE_TIMEOUT_MS = 2500

  const service = {
    /**
     * Adds http:// when no protocol is given and removes trailing slashes.
     * @param {string} address
     * @returns {string}
     */
    normalizeAddress(address) {
      let url = (address || '').trim()
      if (!url) return ''
      if (!/^https?:\/\//i.test(url)) url = `http://${url}`
      return url.replace(/\/+$/, '')
    },

    /**
     * Checks that an Audiobookshelf server answers at the address.
     * @param {string} address
     * @returns {Promise<boolean>}
     */
    async probe(address) {
      try {
        const res = await CapacitorHttp.get({ url: `${address}/ping`, connectTimeout: PROBE_TIMEOUT_MS, readTimeout: PROBE_TIMEOUT_MS })
        return res.status === 200 && !!res.data?.success
      } catch (error) {
        return false
      }
    },

    /**
     * Tests the connection to a server and reports how it went.
     * @param {string} address
     * @param {Object<string,string>} [headers] custom headers (e.g. Cloudflare Access) to send
     * @returns {Promise<{ ok: boolean, ms: number, status?: number, message?: string }>}
     */
    async testConnection(address, headers = {}) {
      const start = Date.now()
      try {
        const res = await CapacitorHttp.get({ url: `${address}/ping`, headers: headers || {}, connectTimeout: 6000, readTimeout: 6000 })
        const ms = Date.now() - start
        if (res.status === 200 && res.data?.success) return { ok: true, ms }
        return { ok: false, ms, status: res.status, message: `HTTP ${res.status}` }
      } catch (error) {
        return { ok: false, ms: Date.now() - start, message: error?.message || 'Could not connect' }
      }
    },

    /**
     * Readable text for a testConnection result.
     * @param {{ ok: boolean, ms: number, status?: number, message?: string }} result
     * @param {Object} strings the $strings object
     */
    describeTest(result, strings) {
      if (result.ok) return `${strings.MessageConnectionOk} (${result.ms} ms)`
      if (result.status === 401 || result.status === 403) return `${strings.MessageConnectionFailed}: ${strings.MessageAccessDenied}`
      return `${strings.MessageConnectionFailed}: ${result.message}`
    },

    /**
     * @param {{ address: string, remoteAddress?: string, localAddress?: string, localSsids?: string[] }} config
     * @returns {Promise<string>} the address that should be used right now
     */
    async resolveAddress(config) {
      const remote = config.remoteAddress || config.address
      if (!config.localAddress) return remote

      const status = await Network.getStatus()
      if (!status.connected || status.connectionType === 'cellular' || status.connectionType === 'none') {
        return remote
      }

      const ssids = config.localSsids || []
      if (ssids.length) {
        try {
          const { ssid } = await AbsRbsSettings.getWifiSsid()
          // When the name cannot be read (no permission / location off) fall back to the ping check below
          if (ssid && !ssids.includes(ssid)) return remote
        } catch (error) {
          console.error('[localNetwork] Failed to read Wi-Fi name', error)
        }
      }

      const reachable = await service.probe(config.localAddress)
      console.log(`[localNetwork] Local address ${config.localAddress} reachable: ${reachable}`)
      return reachable ? config.localAddress : remote
    }
  }

  inject('localNetwork', service)
}
