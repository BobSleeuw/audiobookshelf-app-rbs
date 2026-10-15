import { io } from 'socket.io-client'
import EventEmitter from 'events'
import { createHeaderWebSocketTransport, isNativeWebSocketAvailable } from './nativeWebSocket'

class ServerSocket extends EventEmitter {
  constructor(store) {
    super()

    this.$store = store
    this.socket = null
    this.connected = false
    this.serverAddress = null
    this.isAuthenticated = false

    this.lastReconnectAttemptTime = 0
  }

  /** Reconnect now (app resumed, network back) instead of waiting for the socket.io backoff timer */
  reconnect() {
    if (!this.socket || this.socket.connected) return
    console.log('[SOCKET] Manual reconnect')
    this.socket.connect()
  }

  $on(evt, callback) {
    if (this.socket) this.socket.on(evt, callback)
    else console.error('$on Socket not initialized')
  }

  $off(evt, callback) {
    if (this.socket) this.socket.off(evt, callback)
    else console.error('$off Socket not initialized')
  }

  connect(serverAddress, token) {
    this.serverAddress = serverAddress
    if (this.socket) {
      // Never leave an old socket connected (and reconnecting) behind
      this.socket.disconnect()
      this.removeListeners()
    }

    const serverUrl = new URL(serverAddress)
    const serverHost = `${serverUrl.protocol}//${serverUrl.host}`
    const serverPath = serverUrl.pathname === '/' ? '' : serverUrl.pathname

    console.log(`[SOCKET] Connecting to ${serverHost} with path ${serverPath}/socket.io`)

    // Custom headers (e.g. Cloudflare Access) cannot be sent by a WebView WebSocket, so open it natively
    const customHeaders = this.$store.state.user.serverConnectionConfig?.customHeaders
    const useNativeSocket = isNativeWebSocketAvailable() && customHeaders && Object.keys(customHeaders).length > 0

    const socketOptions = {
      transports: useNativeSocket ? [createHeaderWebSocketTransport(customHeaders)] : ['websocket'],
      upgrade: false,
      path: `${serverPath}/socket.io`,
      reconnection: true,
      reconnectionAttempts: Infinity,
      reconnectionDelay: 1000,
      reconnectionDelayMax: 10000
    }
    this.socket = io(serverHost, socketOptions)
    this.setSocketListeners()
  }

  logout() {
    if (this.socket) this.socket.disconnect()
    this.removeListeners()
  }

  setSocketListeners() {
    this.socket.on('connect', this.onConnect.bind(this))
    this.socket.on('disconnect', this.onDisconnect.bind(this))
    this.socket.on('connect_error', this.onConnectError.bind(this))
    this.socket.on('init', this.onInit.bind(this))
    this.socket.on('auth_failed', this.onAuthFailed.bind(this))
    this.socket.on('user_updated', this.onUserUpdated.bind(this))
    this.socket.on('user_item_progress_updated', this.onUserItemProgressUpdated.bind(this))
    this.socket.on('playlist_added', this.onPlaylistAdded.bind(this))
    this.socket.io.on('reconnect_attempt', this.onReconnectAttempt.bind(this))
    this.socket.io.on('reconnect_error', this.onReconnectError.bind(this))
    this.socket.io.on('reconnect_failed', this.onReconnectFailed.bind(this))
  }

  sendAuthenticate() {
    // Required to connect a socket to a user
    this.socket.emit('auth', this.$store.getters['user/getToken'])
  }

  removeListeners() {
    if (!this.socket) return
    this.socket.removeAllListeners()
    if (this.socket.io && this.socket.io.removeAllListeners) {
      this.socket.io.removeAllListeners()
    }
  }

  onConnect() {
    console.log('[SOCKET] Socket Connected ' + this.socket.id)
    this.connected = true
    this.$store.commit('setSocketConnected', true)
    this.emit('connection-update', true)
    this.sendAuthenticate()
  }

  onConnectError(error) {
    console.log('[SOCKET] Connect error', error?.message)
  }

  onReconnectAttempt(attemptNumber) {
    const timeSinceLastReconnectAttempt = this.lastReconnectAttemptTime ? Date.now() - this.lastReconnectAttemptTime : 0
    this.lastReconnectAttemptTime = Date.now()
    console.log(`[SOCKET] Reconnect attempt ${attemptNumber} ${timeSinceLastReconnectAttempt > 0 ? `after ${timeSinceLastReconnectAttempt}ms` : ''}`)
  }

  onReconnectError(error) {
    console.log('[SOCKET] Reconnect error', error)
  }

  onReconnectFailed(error) {
    console.log('[SOCKET] Reconnect failed', error)
  }

  onDisconnect(reason) {
    console.log('[SOCKET] Socket Disconnected: ' + reason)
    this.connected = false
    this.$store.commit('setSocketConnected', false)
    this.emit('connection-update', false)
    // 'io server disconnect' / 'io client disconnect' are not retried by socket.io on its own
    if (reason === 'io server disconnect' && this.socket) {
      setTimeout(() => this.reconnect(), 2000)
    }
  }

  onInit(data) {
    console.log('[SOCKET] Initial socket data received', data)
    this.emit('initialized', true)
    this.isAuthenticated = true
  }

  onAuthFailed(data) {
    console.log('[SOCKET] Auth failed: ' + (data?.message || 'Unknown reason'))
    this.isAuthenticated = false
  }

  onUserUpdated(data) {
    console.log('[SOCKET] User updated', data)
    this.emit('user_updated', data)
  }

  onUserItemProgressUpdated(payload) {
    console.log('[SOCKET] User Item Progress Updated', JSON.stringify(payload))
    this.$store.commit('user/updateUserMediaProgress', payload.data)
    this.emit('user_media_progress_updated', payload)
  }

  onPlaylistAdded() {
    // Currently numUserPlaylists is only used for showing the playlist tab or not. Precise number is not necessary
    if (!this.$store.state.libraries.numUserPlaylists) {
      this.$store.commit('libraries/setNumUserPlaylists', 1)
    }
  }
}

export default ({ app, store }, inject) => {
  inject('socket', new ServerSocket(store))
}
