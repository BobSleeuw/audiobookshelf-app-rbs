package com.audiobookshelf.app.player

import com.google.android.exoplayer2.ForwardingPlayer
import com.google.android.exoplayer2.Player

/**
 * Wraps the real player for the media session only. When the "chapter" progress setting is on, the position and
 * length that the notification and Android Auto show are those of the current chapter instead of the whole book.
 * Everything else (playback, syncing, the app UI) keeps using the real player.
 */
class ChapterProgressPlayer(player: Player, private val service: PlayerNotificationService) : ForwardingPlayer(player) {
  private fun range(): Pair<Long, Long>? = service.chapterRangeForProgress()

  override fun getCurrentPosition(): Long {
    val r = range() ?: return super.getCurrentPosition()
    return (service.getCurrentTime() - r.first).coerceIn(0L, r.second - r.first)
  }

  override fun getContentPosition(): Long = currentPosition

  override fun getDuration(): Long {
    val r = range() ?: return super.getDuration()
    return r.second - r.first
  }

  override fun getContentDuration(): Long = duration

  override fun getBufferedPosition(): Long {
    val r = range() ?: return super.getBufferedPosition()
    return (service.getBufferedTimeMs() - r.first).coerceIn(0L, r.second - r.first)
  }

  override fun seekTo(positionMs: Long) {
    val r = range()
    if (r == null) super.seekTo(positionMs) else service.seekPlayer(r.first + positionMs)
  }

  override fun seekTo(mediaItemIndex: Int, positionMs: Long) {
    val r = range()
    if (r == null) super.seekTo(mediaItemIndex, positionMs) else service.seekPlayer(r.first + positionMs)
  }
}
