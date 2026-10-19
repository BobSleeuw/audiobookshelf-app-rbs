package com.audiobookshelf.app.media

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Log
import com.audiobookshelf.app.BuildConfig
import com.audiobookshelf.app.device.DeviceManager
import com.audiobookshelf.app.managers.DbManager
import com.audiobookshelf.app.server.CustomHeaders
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileNotFoundException
import java.util.concurrent.TimeUnit

/**
 * ABS-RBS: cover art for Android Auto (feature "aa-cover-art").
 *
 * Android Auto loads cover art itself from the address in the media data. A server address does not work when the
 * server needs custom headers (Cloudflare Access), and a private file of the app cannot be opened by Android Auto
 * either. So the media data points to this provider instead: Android Auto asks the app, and the app gives it the
 * cover file (a downloaded book's own file, or the server cover fetched with the custom headers and kept in the cache).
 *
 * Addresses: content://<applicationId>.covers/server/<library item id>
 *            content://<applicationId>.covers/local/<local library item id>
 *            content://<applicationId>.covers/author/<author id>
 * Only covers and author pictures are served.
 */
class CoverProvider : ContentProvider() {
  companion object {
    private const val TAG = "CoverProvider"
    private const val CACHE_MAX_AGE_MS = 7L * 24 * 60 * 60 * 1000
    private val ID_REGEX = Regex("^[A-Za-z0-9_\\-]{1,100}$")
    private val locks = HashMap<String, Any>()

    private val client: OkHttpClient by lazy {
      OkHttpClient.Builder()
        .addInterceptor(CustomHeaders.Interceptor())
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()
    }

    private fun base() = "content://${BuildConfig.APPLICATION_ID}.covers"

    /** Cover of a book or podcast on the server */
    fun serverItemUri(libraryItemId: String): Uri = Uri.parse("${base()}/server/$libraryItemId")

    /** Cover of a downloaded book or podcast */
    fun localItemUri(localLibraryItemId: String): Uri = Uri.parse("${base()}/local/$localLibraryItemId")

    /** Picture of an author on the server */
    fun authorUri(authorId: String): Uri = Uri.parse("${base()}/author/$authorId")

    private fun lockFor(key: String): Any = synchronized(locks) { locks.getOrPut(key) { Any() } }
  }

  override fun onCreate(): Boolean = true

  override fun getType(uri: Uri): String = "image/*"

  override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor {
    val ctx = context ?: throw FileNotFoundException("No context")
    val segments = uri.pathSegments
    if (segments.size != 2) throw FileNotFoundException("Unknown cover address")
    val kind = segments[0]
    val id = segments[1]
    if (kind !in listOf("server", "local", "author") || !ID_REGEX.matches(id)) throw FileNotFoundException("Unknown cover address")

    DbManager.initialize(ctx)
    val dir = File(ctx.cacheDir, "car_covers").apply { mkdirs() }
    val file = File(dir, "${kind}_$id")

    synchronized(lockFor("$kind/$id")) {
      val outdated = file.exists() && kind != "local" && System.currentTimeMillis() - file.lastModified() > CACHE_MAX_AGE_MS
      if (!file.exists() || file.length() == 0L || outdated) {
        try {
          when (kind) {
            "local" -> copyLocalCover(ctx, id, file)
            "server" -> download("/api/items/$id/cover?width=400&format=jpeg", file)
            else -> download("/api/authors/$id/image?width=300&format=jpeg", file)
          }
        } catch (e: Exception) {
          Log.w(TAG, "No cover for $kind/$id: ${e.message}")
          if (!file.exists() || file.length() == 0L) throw FileNotFoundException("No cover for $kind/$id")
          // An outdated copy is better than no cover
        }
      }
    }
    return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
  }

  /** The cover file of a downloaded book, copied so Android Auto can read it */
  private fun copyLocalCover(ctx: android.content.Context, localLibraryItemId: String, target: File) {
    val item = DeviceManager.dbManager.getLocalLibraryItem(localLibraryItemId) ?: throw FileNotFoundException("Unknown download")
    val url = item.coverContentUrl ?: throw FileNotFoundException("Download without cover")
    val input = ctx.contentResolver.openInputStream(Uri.parse(url)) ?: throw FileNotFoundException("Cover not readable")
    input.use { source -> target.outputStream().use { out -> source.copyTo(out) } }
  }

  /** Fetches a picture from the server with the custom headers (Cloudflare Access) and keeps it in the cache */
  private fun download(path: String, target: File) {
    val config = DeviceManager.serverConnectionConfig ?: DeviceManager.deviceData.getLastServerConnectionConfig()
      ?: throw FileNotFoundException("No server")
    // Servers before 2.17.0 need the token for covers, author pictures always do
    val needsToken = path.startsWith("/api/authors/") || !isNewServer(config.version)
    val url = "${config.address}$path" + if (needsToken) "&token=${config.token}" else ""

    val request = Request.Builder().url(url).addHeader("Authorization", "Bearer ${config.token}").build()
    client.newCall(request).execute().use { response ->
      val body = response.body
      if (!response.isSuccessful || body == null) throw FileNotFoundException("Server answered ${response.code}")
      val temp = File(target.parentFile, "${target.name}.tmp")
      body.byteStream().use { source -> temp.outputStream().use { out -> source.copyTo(out) } }
      if (!temp.renameTo(target)) {
        temp.copyTo(target, overwrite = true)
        temp.delete()
      }
    }
  }

  private fun isNewServer(version: String?): Boolean {
    val parts = (version ?: return false).split(".").map { it.toIntOrNull() ?: 0 }
    return parts.getOrElse(0) { 0 } > 2 || (parts.getOrElse(0) { 0 } == 2 && parts.getOrElse(1) { 0 } >= 17)
  }

  // Read only: nothing can be added, changed or listed through this provider
  override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor? = null

  override fun insert(uri: Uri, values: ContentValues?): Uri? = null

  override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0

  override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
}
