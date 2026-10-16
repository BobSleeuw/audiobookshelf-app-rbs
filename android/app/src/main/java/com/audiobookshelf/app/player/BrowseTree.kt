package com.audiobookshelf.app.player

import android.content.Context
import android.support.v4.media.MediaMetadataCompat
import android.util.Log
import com.audiobookshelf.app.R
import com.audiobookshelf.app.data.*
import com.audiobookshelf.app.media.getUriToDrawable
import com.audiobookshelf.app.settings.RbsSettingsStore

class BrowseTree(
  val context: Context,
  itemsInProgress: List<ItemInProgress>,
  libraries: List<Library>,
  recentsLoaded: Boolean
) {
  private val mediaIdToChildren = mutableMapOf<String, MutableList<MediaMetadataCompat>>()

  init {
    val rootList = mediaIdToChildren[AUTO_BROWSE_ROOT] ?: mutableListOf()

    val continueListeningMetadata = MediaMetadataCompat.Builder().apply {
      putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, CONTINUE_ROOT)
      putString(MediaMetadataCompat.METADATA_KEY_TITLE, "Continue")
      putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, getUriToDrawable(context, R.drawable.exo_icon_localaudio).toString())
    }.build()

    val recentMetadata = MediaMetadataCompat.Builder().apply {
      putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, RECENTLY_ROOT)
      putString(MediaMetadataCompat.METADATA_KEY_TITLE, "Recent")
      putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, getUriToDrawable(context, R.drawable.md_clock_outline).toString())
    }.build()

    val downloadsMetadata = MediaMetadataCompat.Builder().apply {
      putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, DOWNLOADS_ROOT)
      putString(MediaMetadataCompat.METADATA_KEY_TITLE, "Downloads")
      putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, getUriToDrawable(context, R.drawable.exo_icon_downloaddone).toString())
    }.build()

    val librariesMetadata = MediaMetadataCompat.Builder().apply {
      putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, LIBRARIES_ROOT)
      putString(MediaMetadataCompat.METADATA_KEY_TITLE, "Libraries")
      putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, getUriToDrawable(context, R.drawable.icon_library_folder).toString())
    }.build()

    if (itemsInProgress.isNotEmpty()) {
      rootList += continueListeningMetadata
    }

    if (libraries.isNotEmpty()) {
      if (recentsLoaded) {
        rootList += recentMetadata
      }
      rootList += librariesMetadata

      libraries.forEach { library ->
        // Skip libraries without audio content
        if (library.stats?.numAudioFiles == 0) return@forEach
        Log.d("BrowseTree", "Library $library | ${library.icon}")
        // Generate library list items for Libraries menu
        val libraryMediaMetadata = library.getMediaMetadata(context)
        val children = mediaIdToChildren[LIBRARIES_ROOT] ?: mutableListOf()
        children += libraryMediaMetadata
        mediaIdToChildren[LIBRARIES_ROOT] = children

        if (recentsLoaded) {
          // Generate library list items for Recent menu
          val recentlyMediaMetadata = library.getMediaMetadata(context,"recently")
          val childrenRecently = mediaIdToChildren[RECENTLY_ROOT] ?: mutableListOf()
          childrenRecently += recentlyMediaMetadata
          mediaIdToChildren[RECENTLY_ROOT] = childrenRecently
        }
      }
    }

    rootList += downloadsMetadata

    // Volume boost menu (Android Auto boost level), hidden when volume boost is switched off
    if (RbsSettingsStore.volumeBoostEnabled(context)) {
      val boostDb = RbsSettingsStore.androidAutoBoostDb(context)
      val boostMetadata = MediaMetadataCompat.Builder().apply {
        putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, BOOST_ROOT)
        putString(MediaMetadataCompat.METADATA_KEY_TITLE, "Volume boost: ${if (boostDb > 0) "+$boostDb dB" else "Off"}")
        putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, getUriToDrawable(context, boostIcon(boostDb)).toString())
      }.build()
      rootList += boostMetadata
    }

    mediaIdToChildren[AUTO_BROWSE_ROOT] = rootList
  }

  operator fun get(mediaId: String) = mediaIdToChildren[mediaId]
}

const val AUTO_BROWSE_ROOT = "/"
const val CONTINUE_ROOT = "__CONTINUE__"
const val DOWNLOADS_ROOT = "__DOWNLOADS__"
const val LIBRARIES_ROOT = "__LIBRARIES__"
const val RECENTLY_ROOT = "__RECENTLY__"

/** Icon resource for a boost level in dB. */
fun boostIcon(db: Int): Int {
  return when (VolumeBooster.levelIndex(db)) {
    0 -> R.drawable.ic_boost_0
    1 -> R.drawable.ic_boost_1
    2 -> R.drawable.ic_boost_2
    3 -> R.drawable.ic_boost_3
    4 -> R.drawable.ic_boost_4
    else -> R.drawable.ic_boost_5
  }
}
