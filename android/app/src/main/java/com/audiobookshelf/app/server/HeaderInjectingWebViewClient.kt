package com.audiobookshelf.app.server

import android.util.Log
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import com.getcapacitor.Bridge
import com.getcapacitor.BridgeWebViewClient
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * WebView client that adds the user's custom headers (e.g. Cloudflare Access service token) to GET
 * sub-resource requests sent to a saved server, such as `<img>` covers. A WebView cannot add headers to
 * those requests itself, so they are fetched natively and handed back to the WebView.
 */
class HeaderInjectingWebViewClient(bridge: Bridge) : BridgeWebViewClient(bridge) {
  private val tag = "HeaderWebViewClient"
  private val client = OkHttpClient.Builder()
    .connectTimeout(15, TimeUnit.SECONDS)
    .readTimeout(60, TimeUnit.SECONDS)
    .addInterceptor(CustomHeaders.Interceptor())
    .build()

  override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
    val url = request.url.toString()
    val isRemote = url.startsWith("http://") || url.startsWith("https://")
    if (!isRemote || !request.method.equals("GET", true) || CustomHeaders.forUrl(url).isEmpty()) {
      return super.shouldInterceptRequest(view, request)
    }

    return try {
      val builder = Request.Builder().url(url)
      request.requestHeaders?.forEach { (k, v) ->
        // Let OkHttp manage encoding; keep Range so media/image partial loads work
        if (!k.equals("Accept-Encoding", true) && !k.equals("Origin", true)) builder.header(k, v)
      }
      val response = client.newCall(builder.build()).execute()
      val body = response.body
      val contentType = response.header("Content-Type") ?: "application/octet-stream"
      val mime = contentType.substringBefore(";").trim()
      val charset = Regex("charset=([^;]+)").find(contentType)?.groupValues?.get(1)?.trim()
      val headers = mutableMapOf("Access-Control-Allow-Origin" to "*")
      response.header("Content-Range")?.let { headers["Content-Range"] = it }
      response.header("Accept-Ranges")?.let { headers["Accept-Ranges"] = it }
      response.header("Content-Length")?.let { headers["Content-Length"] = it }
      WebResourceResponse(mime, charset, response.code, response.message.ifBlank { "OK" }, headers, body?.byteStream())
    } catch (e: Exception) {
      Log.e(tag, "Failed to fetch $url with custom headers: ${e.message}")
      super.shouldInterceptRequest(view, request)
    }
  }
}
