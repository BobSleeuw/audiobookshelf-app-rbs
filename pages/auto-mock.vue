<template>
  <!-- Android Auto preview: an approximate car screen (clickable) next to the settings that drive it -->
  <div class="w-full h-screen overflow-y-auto p-4 bg-bg text-fg">
    <h1 class="text-xl font-semibold">Android Auto preview</h1>
    <p class="text-sm text-fg-muted mb-4">
      Approximate drawing of the car screen (Google draws the real one from our browse list and buttons, so spacing and fonts differ slightly). Click the tabs, a book, the boost button and the boost levels.
    </p>

    <div class="flex flex-wrap gap-6 items-start">
      <div>
        <div class="rounded-xl border border-fg/20 overflow-hidden" :style="{ width: 960 * scale + 'px', height: 540 * scale + 'px' }">
          <div :style="{ width: '960px', height: '540px', zoom: scale }">
            <automock-screen :level="level" :boost-enabled="boostEnabled" :proposed="proposed" @level="level = $event" />
          </div>
        </div>
        <p v-if="proposed" class="text-xs mt-2" style="color: #f2b84b">Dashed yellow outline = idea that is not built yet.</p>
      </div>

      <div style="width: 300px">
        <p class="uppercase text-xs font-semibold text-fg-muted mb-2">Settings that drive this screen</p>
        <div class="flex items-center py-3 cursor-pointer" @click="boostEnabled = !boostEnabled">
          <p class="flex-grow pr-4">Volume boost (Settings → Playback &amp; audio)</p>
          <ui-toggle-switch :value="boostEnabled" class="pointer-events-none" />
        </div>
        <div class="py-3">
          <div class="flex items-center">
            <p class="flex-grow">Boost level on Android Auto</p>
            <p class="text-sm tabular-nums">{{ level > 0 ? '+' + level + ' dB' : 'Off' }}</p>
          </div>
          <input v-model.number="level" type="range" min="0" max="15" step="3" class="w-full" />
        </div>
        <div class="flex items-center py-3 cursor-pointer" @click="proposed = !proposed">
          <p class="flex-grow pr-4">Show ideas that are not built yet</p>
          <ui-toggle-switch :value="proposed" class="pointer-events-none" />
        </div>
        <p class="text-xs text-fg-muted mt-2">With volume boost off, the boost button and the Volume boost tab disappear from the car screen.</p>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  layout: 'blank',
  data() {
    return { level: 9, boostEnabled: true, proposed: false, scale: 0.85 }
  },
  mounted() {
    // Fit the car screen (960 px wide) into the window
    this.fit = () => (this.scale = Math.min(1, (window.innerWidth - 32) / 960))
    this.fit()
    window.addEventListener('resize', this.fit)
  },
  beforeDestroy() {
    window.removeEventListener('resize', this.fit)
  }
}
</script>
