<template>
  <!-- Android Auto preview: an approximate car screen (clickable) next to the settings that drive it -->
  <div class="w-full h-screen overflow-y-auto p-4 bg-bg text-fg">
    <h1 class="text-xl font-semibold">Android Auto preview</h1>
    <p class="text-sm text-fg-muted mb-4">
      Approximate drawing of the car screen (Google draws the real one from our browse list and buttons, so spacing and fonts differ slightly). Open a book, use the buttons, try the Queue button and the switches below.
    </p>

    <div class="flex flex-wrap gap-6 items-start">
      <div>
        <div class="rounded-xl border border-fg/20 overflow-hidden" :style="{ width: 960 * scale + 'px', height: 540 * scale + 'px' }">
          <div :style="{ width: '960px', height: '540px', zoom: scale }">
            <automock-screen :level="level" :boost-enabled="boostEnabled" :boost-style="boostStyle" :proposed="proposed" :chapter-progress="chapterProgress" :chapters-in-queue="chaptersInQueue" :covers="covers" @level="level = $event" />
          </div>
        </div>
        <p v-if="boostStyle !== 'toggle' || proposed" class="text-xs mt-2" style="color: #f2b84b">Dashed yellow outline = new, not built yet.</p>

        <!-- the small media card on the car's dashboard -->
        <p class="uppercase text-xs font-semibold text-fg-muted mt-6 mb-2">The small card on the car's dashboard</p>
        <div class="rounded-2xl flex items-center p-3" style="width: 380px; background: #2a2d33">
          <div class="rounded-lg flex-shrink-0 flex items-center justify-center" :style="{ width: '96px', height: '96px', background: covers ? 'linear-gradient(135deg, #8b2c2c, #5b1a1a)' : '#3c4043' }">
            <span v-if="!covers" class="material-symbols text-4xl" style="color: #9aa0a6">music_note</span>
          </div>
          <div class="ml-4 min-w-0" style="color: #e8eaed">
            <p class="truncate">Project Hail Mary</p>
            <p class="text-sm truncate" style="color: #9aa0a6">Andy Weir</p>
            <div class="rounded-full mt-3" style="height: 4px; width: 200px; background: #3c4043"><div class="rounded-full" style="height: 4px; width: 62%; background: #8ab4f8" /></div>
          </div>
        </div>
      </div>

      <div style="width: 340px">
        <p class="uppercase text-xs font-semibold text-fg-muted mb-2">Settings that drive this screen</p>
        <div class="flex items-center py-3 cursor-pointer" @click="boostEnabled = !boostEnabled">
          <p class="flex-grow pr-4">Volume boost (Settings → Playback &amp; audio)</p>
          <ui-toggle-switch :value="boostEnabled" class="pointer-events-none" />
        </div>
        <div class="py-3">
          <div class="flex items-center">
            <p class="flex-grow">Boost level on Android Auto (slider in the app)</p>
            <p class="text-sm tabular-nums">{{ level > 0 ? '+' + level + ' dB' : 'Off' }}</p>
          </div>
          <input v-model.number="level" type="range" min="0" max="20" step="1" class="w-full" />
        </div>
        <div class="py-3">
          <p class="mb-2">Progress bar (Connections &amp; data → Android Auto)</p>
          <ui-toggle-btns v-model="progressMode" :items="[{ text: 'Chapter Track', value: 'chapter' }, { text: 'Total Track', value: 'book' }]" />
        </div>

        <p class="uppercase text-xs font-semibold text-fg-muted mt-4 mb-2">The boost button in the car (decided)</p>
        <p class="text-xs text-fg-muted">One button. Each tap raises the boost by 2 dB: Off, +2, +4 ... +20, then Off again. It changes the same level as the slider above. Try it in the car screen.</p>

        <p class="uppercase text-xs font-semibold text-fg-muted mt-5 mb-2">Queue button (Connections &amp; data → Android Auto → Queue shows)</p>
        <ui-toggle-btns v-model="queueMode" :items="[{ text: 'Files (now)', value: 'files' }, { text: 'Chapters', value: 'chapters' }]" />
        <p class="text-xs text-fg-muted mt-2">Android Auto has one fixed button, always called "Queue". We cannot add a second button next to it, but we can fill that list with the chapters and give the list the title "Chapters". Tapping a chapter jumps to it.</p>

        <div class="flex items-center py-3 mt-3 cursor-pointer" @click="covers = !covers">
          <p class="flex-grow pr-4">Cover art shows up</p>
          <ui-toggle-switch :value="covers" class="pointer-events-none" />
        </div>
        <p class="text-xs text-fg-muted">Off = what your car shows today. On = after the cover art fix (see below).</p>

        <div class="flex items-center py-3 mt-3 cursor-pointer" @click="proposed = !proposed">
          <p class="flex-grow pr-4">Show other ideas (sleep timer, bookmark, Series tab)</p>
          <ui-toggle-switch :value="proposed" class="pointer-events-none" />
        </div>
      </div>

      <div style="max-width: 520px">
        <p class="uppercase text-xs font-semibold text-fg-muted mb-2">Why there is no slider in the car</p>
        <p class="text-sm">Android Auto draws media apps itself and only allows buttons, lists and a progress bar. Apps cannot add sliders, and there is no settings screen for media apps in the car (a safety rule). So the level is set with the slider in the phone app, and the car gets the best substitute. The four options:</p>
        <ul class="list-disc pl-5 text-sm space-y-1 mt-2">
          <li><b>On/off</b> (what you have): one button, no way to change the level.</li>
          <li><b>− and +</b> (my suggestion): two buttons, 1 dB per tap. The level is shown on the screen. Needs one more button; if the car runs out of room, Android Auto moves extra buttons into a "more" menu.</li>
          <li><b>Step</b>: one button, each tap +1 dB, after 20 it goes back to Off. Compact, but going down takes many taps.</li>
          <li><b>List</b>: a "Volume boost" tab with Louder / Quieter / Off rows. It uses one of the 4 tabs Android Auto allows (it would replace Recent).</li>
        </ul>

        <p class="uppercase text-xs font-semibold text-fg-muted mt-6 mb-2">Why the book covers are missing</p>
        <p class="text-sm">For books on your server, the car itself has to download the cover from your server. Your server is behind the Cloudflare headers, and the car cannot send those, so it gets nothing and shows grey. Downloaded books work because the app hands over the picture directly. For the small dashboard card, the car also cannot open the app's private file for the cover.</p>
        <p class="text-sm mt-2"><b>Fix:</b> give the app its own small cover provider. The car asks the app for the cover, and the app fetches it with the headers (and caches it). That would fix the browse lists, the queue and the dashboard card in one go.</p>
        <p class="text-xs text-fg-muted mt-2">This is my best explanation from reading the code; I cannot see the car, so I will check it with the logs after the fix.</p>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  layout: 'blank',
  data() {
    return { level: 9, boostEnabled: true, boostStyle: 'step', proposed: false, progressMode: 'chapter', queueMode: 'chapters', covers: false, scale: 0.85 }
  },
  computed: {
    chapterProgress() {
      return this.progressMode === 'chapter'
    },
    chaptersInQueue() {
      return this.queueMode === 'chapters'
    },
    boostStyleText() {
      return {
        toggle: 'One button switches the boost on and off at the level from the phone slider. This is the version you have now.',
        plusminus: 'Two buttons next to each other: − lowers and + raises the boost by 1 dB. The level is also shown under the buttons.',
        step: 'One button. Each tap raises the boost by 1 dB, after +20 it goes back to Off.',
        list: 'A "Volume boost" tab with the rows Louder, Quieter and Off. It takes one of the four tab places.'
      }[this.boostStyle]
    }
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
