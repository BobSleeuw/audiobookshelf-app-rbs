/**
 * Playback history: remembers where you were before a big jump (progress bar, chapter skip, a long seek from the
 * notification or Android Auto) so you can go back. Stored on the phone, per book.
 *
 * Jumps are found by comparing every new playback position with where playback should be by now.
 */
const MAX_ENTRIES = 10
const BAR_SECONDS = 7

export default {
  data() {
    return {
      historySettings: { enabled: true, minSeconds: 60, showBar: true },
      historyEntries: [],
      historyBar: null,
      showHistoryModal: false,
      historyRef: null,
      historyReason: null,
      historyBarTimer: null
    }
  },
  computed: {
    historyAvailable() {
      return this.historySettings.enabled && !!this.mediaId
    }
  },
  methods: {
    async loadHistorySettings() {
      this.historySettings = await this.$localStore.getPlaybackHistorySettings()
    },
    onHistorySettingsChanged(settings) {
      this.historySettings = { ...settings }
      if (!settings.enabled || !settings.showBar) this.historyBar = null
      this.historyRef = null
    },
    async loadHistoryEntries() {
      this.historyEntries = this.mediaId ? await this.$localStore.getPlaybackHistory(this.mediaId) : []
    },
    /** A new book started: forget the last position and load this book's history */
    historyNewSession() {
      this.historyRef = null
      this.historyBar = null
      this.historyReason = null
      this.loadHistoryEntries()
    },
    historyReset(time) {
      this.historyRef = { time, at: Date.now() }
    },
    /** What caused the next jump (set just before a seek from the player) */
    historyMarkReason(reason, icon) {
      this.historyReason = { reason, icon, at: Date.now() }
    },
    /** Call with every new playback position */
    historyObserve(time) {
      if (!this.historyAvailable) {
        this.historyRef = null
        return
      }
      const ref = this.historyRef
      this.historyReset(time)
      if (!ref) return

      const elapsed = (Date.now() - ref.at) / 1000
      const expected = this.isPlaying ? elapsed * this.currentPlaybackRate : 0
      if (Math.abs(time - ref.time - expected) < this.historySettings.minSeconds) return

      const from = Math.min(Math.max(0, ref.time + expected), this.totalDuration || Infinity)
      this.historyRecord(from, time)
    },
    historyRecord(from, to) {
      const cause = this.historyReason && Date.now() - this.historyReason.at < 15000 ? this.historyReason : null
      this.historyReason = null
      const entry = {
        id: Date.now(),
        from,
        to,
        at: Date.now(),
        reason: cause ? cause.reason : this.$strings.LabelHistoryReasonOther,
        icon: cause ? cause.icon : 'fast_forward'
      }
      // The same place twice in a row is not interesting
      this.historyEntries = [entry, ...this.historyEntries.filter((e) => Math.abs(e.from - from) > 5)].slice(0, MAX_ENTRIES)
      this.saveHistoryEntries()

      if (this.historySettings.showBar && this.showFullscreen) {
        this.historyBar = entry
        clearTimeout(this.historyBarTimer)
        this.historyBarTimer = setTimeout(() => (this.historyBar = null), BAR_SECONDS * 1000)
      }
    },
    saveHistoryEntries() {
      if (this.mediaId) this.$localStore.setPlaybackHistory(this.mediaId, this.historyEntries)
    },
    historyGoBack(entry) {
      this.historyMarkReason(this.$strings.LabelHistoryReasonUndo, 'undo')
      this.historyBar = null
      this.showHistoryModal = false
      this.seek(entry.from)
    },
    historyClear() {
      this.historyEntries = []
      this.saveHistoryEntries()
    }
  }
}
