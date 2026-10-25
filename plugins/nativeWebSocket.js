import { registerPlugin, Capacitor } from '@capacitor/core'
import { WebSocket as EngineIoWebSocket } from 'engine.io-client'

/**
 * A WebView WebSocket cannot send custom headers (e.g. Cloudflare Access service token). This provides a socket.io
 * transport that opens the websocket natively (OkHttp) with the headers and relays the events back.
 */
const AbsWebSocket = registerPlugin('AbsWebSocket')

/** @type {Object<string, NativeWebSocket>} */
const sockets = {}
let listenerRegistered = false
let nextId = 1

function base64ToArrayBuffer(base64) {
  const bin = atob(base64)
  const bytes = new Uint8Array(bin.length)
  for (let i = 0; i < bin.length; i++) bytes[i] = bin.charCodeAt(i)
  return bytes.buffer
}

function arrayBufferToBase64(buffer) {
  const bytes = new Uint8Array(buffer)
  let bin = ''
  for (let i = 0; i < bytes.length; i++) bin += String.fromCharCode(bytes[i])
  return btoa(bin)
}

function ensureListener() {
  if (listenerRegistered) return
  listenerRegistered = true
  AbsWebSocket.addListener('wsEvent', (event) => {
    sockets[event.id]?.handleEvent(event)
  })
}

/** Minimal WebSocket compatible object, as used by engine.io's websocket transport */
class NativeWebSocket {
  constructor(url, headers) {
    ensureListener()
    this.id = `ws${nextId++}`
    this.url = url
    this.readyState = 0 // CONNECTING
    this.binaryType = 'arraybuffer'
    this.onopen = null
    this.onmessage = null
    this.onclose = null
    this.onerror = null
    sockets[this.id] = this

    AbsWebSocket.open({ id: this.id, url, headers }).catch((error) => {
      this.handleEvent({ type: 'error', message: error?.message })
      this.handleEvent({ type: 'close', code: 1006, reason: error?.message || '' })
    })
  }

  handleEvent(event) {
    if (event.type === 'open') {
      this.readyState = 1
      this.onopen?.({})
    } else if (event.type === 'message') {
      const data = event.binary ? base64ToArrayBuffer(event.data) : event.data
      this.onmessage?.({ data })
    } else if (event.type === 'error') {
      this.onerror?.({ message: event.message })
    } else if (event.type === 'close') {
      if (this.readyState === 3) return
      this.readyState = 3
      delete sockets[this.id]
      this.onclose?.({ code: event.code, reason: event.reason, wasClean: event.code === 1000 })
    }
  }

  send(data) {
    if (this.readyState !== 1) throw new Error('WebSocket is not open')
    const payload = typeof data === 'string' ? { id: this.id, data } : { id: this.id, base64: arrayBufferToBase64(data instanceof ArrayBuffer ? data : data.buffer) }
    AbsWebSocket.send(payload).catch((error) => console.error('[NativeWebSocket] send failed', error))
  }

  close(code, reason) {
    if (this.readyState === 3) return
    this.readyState = 2
    AbsWebSocket.close({ id: this.id, code, reason }).catch(() => {})
  }
}

/** Whether the native websocket can be used (needs the Android plugin) */
export function isNativeWebSocketAvailable() {
  return Capacitor.getPlatform() === 'android'
}

/**
 * Creates a socket.io transport class that sends [headers] with the websocket upgrade request.
 * @param {Object<string,string>} headers
 */
export function createHeaderWebSocketTransport(headers) {
  return class HeaderWebSocket extends EngineIoWebSocket {
    createSocket(uri) {
      return new NativeWebSocket(uri, headers)
    }
  }
}
