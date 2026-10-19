package com.audiobookshelf.app.server

import android.net.Uri
import com.audiobookshelf.app.device.DeviceManager
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.LazyHeaders
import okhttp3.Interceptor
import okhttp3.Response

/**
 * User-defined headers (e.g. Cloudflare Access `CF-Access-Client-Id` / `CF-Access-Client-Secret`)
 * that are attached to every request sent to the active server.
 */
object CustomHeaders {
  /** Headers for the active server, or empty when none are configured. */
  fun forActiveServer(): Map<String, String> {
    return DeviceManager.serverConnectionConfig?.customHeaders?.filter { it.key.isNotBlank() } ?: emptyMap()
  }

  /** The hosts of all addresses of a server: the one in use now, the external and the internal address. */
  private fun hostsOf(config: com.audiobookshelf.app.data.ServerConnectionConfig): List<String?> {
    return listOf(config.address, config.remoteAddress, config.localAddress).filter { !it.isNullOrBlank() }.map { Uri.parse(it).host }
  }

  /** True when [host] is the host of any address of a saved server (headers are never sent to other hosts, e.g. CDNs). */
  private fun isServerHost(host: String?): Boolean {
    if (host == null) return false
    return DeviceManager.deviceData.serverConnectionConfigs.any { hostsOf(it).contains(host) }
  }

  /** Headers to use for [url]; only returned for hosts belonging to a saved server. */
  fun forUrl(url: String): Map<String, String> {
    val host = Uri.parse(url).host
    if (!isServerHost(host)) return emptyMap()
    val active = DeviceManager.serverConnectionConfig
    val config = if (active != null && hostsOf(active).contains(host)) active
    else DeviceManager.deviceData.serverConnectionConfigs.firstOrNull { hostsOf(it).contains(host) }
    return config?.customHeaders?.filter { it.key.isNotBlank() } ?: emptyMap()
  }

  /** Wraps [url] for Glide with the custom headers of the matching server. */
  fun glideModel(url: String): Any {
    val headers = forUrl(url)
    if (headers.isEmpty()) return url
    val builder = LazyHeaders.Builder()
    headers.forEach { (k, v) -> builder.addHeader(k, v) }
    return GlideUrl(url, builder.build())
  }

  /** OkHttp interceptor adding the custom headers to requests that target a saved server. */
  class Interceptor : okhttp3.Interceptor {
    override fun intercept(chain: okhttp3.Interceptor.Chain): Response {
      val request = chain.request()
      val headers = forUrl(request.url.toString())
      if (headers.isEmpty()) return chain.proceed(request)
      val builder = request.newBuilder()
      headers.forEach { (k, v) -> if (request.header(k) == null) builder.header(k, v) }
      return chain.proceed(builder.build())
    }
  }
}
