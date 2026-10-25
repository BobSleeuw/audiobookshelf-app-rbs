package com.audiobookshelf.app.plugins

import android.util.Base64
import android.util.Log
import com.getcapacitor.JSObject
import com.getcapacitor.Plugin
import com.getcapacitor.PluginCall
import com.getcapacitor.PluginMethod
import com.getcapacitor.annotation.CapacitorPlugin
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import okio.ByteString.Companion.toByteString
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Native WebSocket for the web layer. A WebView WebSocket cannot send custom request headers, which are needed
 * to pass proxies such as Cloudflare Access. The web layer talks to this plugin instead (see plugins/nativeWebSocket.js)
 * and receives socket events through `wsEvent` listeners.
 */
@CapacitorPlugin(name = "AbsWebSocket")
class AbsWebSocket : Plugin() {
  private val tag = "AbsWebSocket"
  private val sockets = ConcurrentHashMap<String, WebSocket>()
  private val client = OkHttpClient.Builder()
    .connectTimeout(20, TimeUnit.SECONDS)
    .readTimeout(0, TimeUnit.MILLISECONDS) // engine.io sends its own pings
    .build()

  private fun emit(id: String, type: String, block: JSObject.() -> Unit = {}) {
    val data = JSObject().put("id", id).put("type", type)
    data.block()
    notifyListeners("wsEvent", data)
  }

  @PluginMethod
  fun open(call: PluginCall) {
    val id = call.getString("id")
    val url = call.getString("url")
    if (id == null || url == null) {
      call.reject("id and url required")
      return
    }
    try {
      val builder = Request.Builder().url(url)
      call.getObject("headers")?.let { headers ->
        headers.keys().forEach { builder.header(it, headers.getString(it) ?: "") }
      }
      val ws = client.newWebSocket(builder.build(), object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
          emit(id, "open")
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
          emit(id, "message") { put("data", text).put("binary", false) }
        }

        override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
          emit(id, "message") { put("data", Base64.encodeToString(bytes.toByteArray(), Base64.NO_WRAP)).put("binary", true) }
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
          webSocket.close(code, reason)
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
          sockets.remove(id)
          emit(id, "close") { put("code", code).put("reason", reason) }
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
          Log.e(tag, "WebSocket $id failed: ${t.message} (status ${response?.code})")
          sockets.remove(id)
          emit(id, "error") { put("message", t.message ?: "WebSocket failure").put("status", response?.code ?: 0) }
          emit(id, "close") { put("code", 1006).put("reason", t.message ?: "") }
        }
      })
      sockets[id] = ws
      call.resolve()
    } catch (e: Exception) {
      Log.e(tag, "Failed to open WebSocket: ${e.message}")
      call.reject(e.message ?: "Failed to open WebSocket")
    }
  }

  @PluginMethod
  fun send(call: PluginCall) {
    val ws = call.getString("id")?.let { sockets[it] }
    if (ws == null) {
      call.reject("socket not open")
      return
    }
    val base64 = call.getString("base64")
    val ok = if (base64 != null) {
      ws.send(Base64.decode(base64, Base64.DEFAULT).toByteString())
    } else {
      ws.send(call.getString("data") ?: "")
    }
    if (ok) call.resolve() else call.reject("send failed")
  }

  @PluginMethod
  fun close(call: PluginCall) {
    val id = call.getString("id")
    val ws = id?.let { sockets.remove(it) }
    ws?.close(call.getInt("code", 1000) ?: 1000, call.getString("reason"))
    if (id != null && ws != null) emit(id, "close") { put("code", 1000).put("reason", "closed by client") }
    call.resolve()
  }
}
