<template>
  <!-- Mockup: player screen with "playback history" (go back after an accidental jump) -->
  <div class="relative w-full h-full flex flex-col overflow-hidden bg-bg text-fg">
    <!-- app bar -->
    <div class="flex items-center h-14 px-3 bg-primary flex-shrink-0">
      <span class="material-symbols text-2xl mr-2">keyboard_arrow_down</span>
      <p class="flex-grow truncate text-sm text-fg-muted">Now playing</p>
      <!-- the new button: opens the playback history -->
      <button type="button" class="relative h-10 w-10 flex items-center justify-center rounded-full" :class="highlight ? 'ring-2 ring-success' : ''" @click="showHistory = true">
        <span class="material-symbols text-2xl">history</span>
        <span v-if="entries.length" class="absolute top-1 right-1 min-w-4 h-4 px-1 rounded-full bg-success text-white text-[10px] leading-4 text-center">{{ entries.length }}</span>
      </button>
    </div>

    <div class="flex-grow overflow-y-auto px-5 pt-5">
      <div class="mx-auto rounded-lg shadow-lg" style="width: 190px; height: 190px; background: linear-gradient(135deg, #8b2c2c, #5b1a1a)" />
      <p class="text-center text-lg mt-4 truncate">Project Hail Mary</p>
      <p class="text-center text-sm text-fg-muted truncate">Andy Weir</p>
      <p class="text-center text-sm mt-1 truncate">{{ chapterOf(pos).title }}</p>

      <!-- progress bar: drag it to try a jump -->
      <div class="mt-5">
        <input :value="pos" type="range" min="0" :max="TOTAL" step="1" class="w-full" @change="dragTo(Number($event.target.value))" @input="pos = Number($event.target.value)" />
        <div class="flex justify-between text-xs text-fg-muted -mt-0.5">
          <span>{{ fmt(pos) }}</span><span>-{{ fmt(TOTAL - pos) }}</span>
        </div>
      </div>

      <!-- controls -->
      <div class="flex items-center justify-center space-x-3 mt-4">
        <button type="button" class="ctl" @click="move(-10, 'Jump back')"><span class="material-symbols text-2xl">replay_10</span></button>
        <button type="button" class="ctl" @click="chapterStep(-1)"><span class="material-symbols text-3xl">skip_previous</span></button>
        <button type="button" class="ctl ctl-play" @click="playing = !playing"><span class="material-symbols text-4xl" style="color: #232323">{{ playing ? 'pause' : 'play_arrow' }}</span></button>
        <button type="button" class="ctl" @click="chapterStep(1)"><span class="material-symbols text-3xl">skip_next</span></button>
        <button type="button" class="ctl" @click="move(30, 'Jump forward')"><span class="material-symbols text-2xl">forward_30</span></button>
      </div>

      <p class="text-xs text-fg-muted text-center mt-6">Try it: drag the bar a long way, or tap next / previous chapter. Short jumps (under {{ MIN_JUMP }} s) are not recorded.</p>
    </div>

    <!-- "Moved ... go back" bar that appears after a big jump -->
    <transition name="fade">
      <div v-if="toast" class="absolute left-3 right-3 bottom-4 rounded-lg bg-secondary border border-fg/20 shadow-lg flex items-center px-4 py-3 z-10">
        <p class="flex-grow text-sm">Moved to {{ fmt(toast.to) }}</p>
        <button type="button" class="text-success font-semibold text-sm pl-4" @click="goBackTo(toast)">Go back to {{ fmt(toast.from) }}</button>
      </div>
    </transition>

    <!-- history sheet -->
    <div v-if="showHistory" class="absolute inset-0 z-20 flex flex-col justify-end bg-black bg-opacity-60" @click="showHistory = false">
      <div class="bg-primary rounded-t-2xl max-h-[85%] flex flex-col" @click.stop>
        <div class="flex items-center px-5 pt-4 pb-2">
          <p class="text-lg flex-grow">Playback history</p>
          <button v-if="entries.length" type="button" class="text-sm text-fg-muted" @click="entries = []">Clear</button>
        </div>
        <p class="px-5 pb-2 text-xs text-fg-muted">Places you were before a big jump. Tap one to return to it.</p>

        <div class="overflow-y-auto px-2 pb-4">
          <p v-if="!entries.length" class="px-3 py-8 text-center text-fg-muted">Nothing yet. Skip around a bit and come back.</p>
          <div v-for="entry in entries" :key="entry.id" class="flex items-center px-3 py-3 rounded-lg cursor-pointer active:bg-fg/10" @click="goBackTo(entry)">
            <span class="material-symbols text-2xl text-fg-muted w-10">{{ entry.icon }}</span>
            <div class="flex-grow min-w-0">
              <p class="truncate">{{ fmt(entry.from) }} · {{ chapterOf(entry.from).title }}</p>
              <p class="text-xs text-fg-muted truncate">{{ entry.reason }} · {{ ago(entry.at) }}</p>
            </div>
            <span class="material-symbols text-fg-muted">undo</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
const CHAPTERS = [
  { title: 'Chapter 1', start: 0 },
  { title: 'Chapter 2', start: 2300 },
  { title: 'Chapter 3', start: 5200 },
  { title: 'Chapter 4', start: 8100 },
  { title: 'Chapter 5', start: 10900 },
  { title: 'Chapter 6', start: 13330 - 600 },
  { title: 'Chapter 7', start: 15500 },
  { title: 'Chapter 8', start: 18400 },
  { title: 'Chapter 9', start: 21600 },
  { title: 'Chapter 10', start: 24800 },
  { title: 'Chapter 11', start: 28000 },
  { title: 'Chapter 12', start: 31500 }
]
const TOTAL = 34830

export default {
  data() {
    return { TOTAL, MIN_JUMP: 60, pos: 13330, playing: true, entries: [], toast: null, toastTimer: null, showHistory: false, highlight: false, nextId: 1, now: Date.now() }
  },
  methods: {
    fmt(s) {
      s = Math.max(0, Math.round(s))
      const h = Math.floor(s / 3600)
      const m = Math.floor((s % 3600) / 60)
      const sec = s % 60
      return `${h}:${String(m).padStart(2, '0')}:${String(sec).padStart(2, '0')}`
    },
    chapterOf(s) {
      let current = CHAPTERS[0]
      CHAPTERS.forEach((c) => {
        if (s >= c.start) current = c
      })
      return current
    },
    ago(at) {
      const minutes = Math.round((this.now - at) / 60000)
      return minutes < 1 ? 'just now' : `${minutes} min ago`
    },
    /** Records where you were when a jump was big enough, and shows the "go back" bar */
    record(from, to, reason, icon) {
      if (Math.abs(to - from) < this.MIN_JUMP) return
      this.now = Date.now()
      const entry = { id: this.nextId++, from, to, reason, icon, at: this.now }
      // The same place twice in a row is not interesting
      this.entries = [entry, ...this.entries.filter((e) => Math.abs(e.from - from) > 5)].slice(0, 10)
      this.toast = entry
      this.highlight = true
      clearTimeout(this.toastTimer)
      this.toastTimer = setTimeout(() => {
        this.toast = null
        this.highlight = false
      }, 7000)
    },
    move(delta, reason) {
      const from = this.pos
      this.pos = Math.min(TOTAL, Math.max(0, this.pos + delta))
      this.record(from, this.pos, reason, 'fast_forward')
    },
    chapterStep(direction) {
      const from = this.pos
      const index = CHAPTERS.indexOf(this.chapterOf(from))
      const target = CHAPTERS[Math.min(CHAPTERS.length - 1, Math.max(0, index + direction))]
      this.pos = target.start
      this.record(from, this.pos, direction > 0 ? 'Skipped to the next chapter' : 'Skipped to the previous chapter', direction > 0 ? 'skip_next' : 'skip_previous')
    },
    dragTo(value) {
      // the slider already moved the bar while dragging; the place it was released from is remembered in dragFrom
      const from = this.dragFrom !== undefined ? this.dragFrom : this.pos
      this.pos = value
      this.record(from, value, 'Moved the progress bar', 'linear_scale')
      this.dragFrom = undefined
    },
    goBackTo(entry) {
      const from = this.pos
      this.pos = entry.from
      this.showHistory = false
      this.toast = null
      // going back is a jump too, so it can be undone
      this.record(from, this.pos, 'Went back in history', 'undo')
      this.toast = null
    }
  },
  mounted() {
    // remember where a drag started (the range input updates the position while dragging)
    this.$el.addEventListener('pointerdown', (e) => {
      if (e.target.type === 'range') this.dragFrom = this.pos
    })
  }
}
</script>

<style>
.ctl {
  width: 48px;
  height: 48px;
  border-radius: 9999px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.ctl-play {
  width: 64px;
  height: 64px;
  background: rgb(230, 237, 243);
}
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.25s;
}
.fade-enter,
.fade-leave-to {
  opacity: 0;
}
</style>
