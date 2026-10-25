package com.audiobookshelf.app.player

import android.os.Bundle
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.ext.mediasession.TimelineQueueNavigator
import android.os.ResultReceiver

/**
 * The queue Android Auto shows behind its "Queue" button. An audiobook is usually one file, so the normal queue
 * (the files) has a single entry. With the "chapters" setting the queue lists the chapters instead (titled
 * "Chapters"), the current chapter is highlighted and tapping one jumps to it. Without chapters, or with the
 * "files" setting, it behaves like before.
 */
class ChapterQueueNavigator(
  private val mediaSession: MediaSessionCompat,
  private val files: TimelineQueueNavigator,
  private val service: PlayerNotificationService
) : MediaSessionConnector.QueueNavigator {
  private var chapterQueuePublished = false

  override fun getSupportedQueueNavigatorActions(player: Player): Long {
    val actions = files.getSupportedQueueNavigatorActions(player)
    return if (service.chapterQueueActive()) actions or PlaybackStateCompat.ACTION_SKIP_TO_QUEUE_ITEM else actions
  }

  override fun onTimelineChanged(player: Player) {
    publish(player)
  }

  override fun onCurrentMediaItemIndexChanged(player: Player) {
    if (service.chapterQueueActive()) publish(player) else files.onCurrentMediaItemIndexChanged(player)
  }

  override fun getActiveQueueItemId(player: Player?): Long {
    return if (service.chapterQueueActive()) service.currentChapterIndex().toLong() else files.getActiveQueueItemId(player)
  }

  override fun onSkipToPrevious(player: Player) {
    service.skipToPrevious()
  }

  override fun onSkipToQueueItem(player: Player, id: Long) {
    if (service.chapterQueueActive()) service.skipToChapter(id.toInt()) else files.onSkipToQueueItem(player, id)
  }

  override fun onSkipToNext(player: Player) {
    service.skipToNext()
  }

  override fun onCommand(player: Player, command: String, extras: Bundle?, cb: ResultReceiver?): Boolean {
    return files.onCommand(player, command, extras, cb)
  }

  /** Publishes the chapters, or hands the queue back to the file list */
  private fun publish(player: Player) {
    if (service.chapterQueueActive()) {
      mediaSession.setQueue(service.chapterQueueItems())
      mediaSession.setQueueTitle("Chapters")
      chapterQueuePublished = true
    } else {
      if (chapterQueuePublished) {
        mediaSession.setQueueTitle(null)
        chapterQueuePublished = false
      }
      files.onTimelineChanged(player)
    }
  }
}
