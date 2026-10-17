<template>
  <!-- Approximate Android Auto media screen (what Google draws from our browse tree + playback buttons) -->
  <div class="aa-screen flex w-full h-full overflow-hidden text-white select-none relative" style="background: #131417; font-family: 'Google Sans', Roboto, sans-serif">
    <!-- left rail (Android Auto navigation) -->
    <div class="flex flex-col items-center justify-between py-3 flex-shrink-0" style="width: 64px; background: #0c0d0f">
      <div class="flex flex-col items-center space-y-5 pt-2">
        <span class="material-symbols text-2xl" style="color: #9aa0a6">navigation</span>
        <span class="material-symbols text-2xl" style="color: #9aa0a6">call</span>
        <span class="material-symbols text-2xl rounded-full p-2" style="background: #2b3a55; color: #8ab4f8">headphones</span>
        <span class="material-symbols text-2xl" style="color: #9aa0a6">notifications</span>
      </div>
      <span class="material-symbols text-2xl" style="color: #9aa0a6">grid_view</span>
    </div>

    <!-- app area -->
    <div class="flex-grow flex flex-col min-w-0">
      <!-- top bar: back, app name, tabs (browse) or Queue button (now playing) -->
      <div class="flex items-center px-5 flex-shrink-0" style="height: 56px; border-bottom: 1px solid #26282c">
        <span v-if="screen !== 'browse'" class="material-symbols text-2xl mr-3 cursor-pointer" style="color: #9aa0a6" @click="back">arrow_back</span>
        <p class="text-lg font-medium mr-6">ABS-RBS</p>
        <div v-if="screen === 'browse'" class="flex space-x-1">
          <div v-for="tab in tabs" :key="tab.id" class="px-4 py-1.5 rounded-full cursor-pointer text-sm" :class="tab.proposed ? 'aa-proposed' : ''" :style="activeTab === tab.id ? 'background:#2b3a55;color:#8ab4f8' : 'color:#c4c7cc'" @click="activeTab = tab.id">
            <span class="material-symbols align-middle mr-1" style="font-size: 18px">{{ tab.icon }}</span>{{ tab.title }}
          </div>
        </div>
        <template v-else>
          <div class="flex-grow" />
          <!-- the standard "Queue" button of Android Auto -->
          <div class="rounded-full px-4 py-1.5 text-sm cursor-pointer flex items-center" style="border: 1px solid #8ab4f8; color: #e8eaed" @click="screen = screen === 'queue' ? 'playing' : 'queue'">
            <span class="material-symbols mr-1" style="font-size: 18px">queue_music</span>Queue
          </div>
        </template>
      </div>

      <!-- browse: list of books -->
      <div v-if="screen === 'browse' && activeTab !== 'boostlist'" class="flex-grow overflow-y-auto px-5 py-3">
        <div v-for="book in books" :key="book.title" class="flex items-center py-2.5 cursor-pointer" @click="screen = 'playing'">
          <div class="rounded-lg mr-4 flex-shrink-0 flex items-center justify-center" :style="{ width: '52px', height: '52px', background: covers ? book.color : '#3c4043' }">
            <span v-if="!covers" class="material-symbols" style="color: #9aa0a6">music_note</span>
          </div>
          <div class="min-w-0 flex-grow">
            <p class="text-base truncate">{{ book.title }}</p>
            <p class="text-sm truncate" style="color: #9aa0a6">{{ book.author }} · {{ book.progress }}</p>
          </div>
        </div>
      </div>

      <!-- browse: settings list (option 4 for the boost level) -->
      <div v-else-if="screen === 'browse'" class="flex-grow overflow-y-auto px-5 py-3">
        <p class="text-xs uppercase mb-2" style="color: #9aa0a6">Volume boost</p>
        <div class="flex items-center py-3 cursor-pointer" @click="$emit('level', Math.min(20, level + 1))">
          <span class="material-symbols text-3xl mr-4" style="color: #8ab4f8">add_circle</span>
          <p class="text-base flex-grow">Louder <span style="color: #9aa0a6">(+1 dB)</span></p>
        </div>
        <div class="flex items-center py-3 cursor-pointer" @click="$emit('level', Math.max(0, level - 1))">
          <span class="material-symbols text-3xl mr-4" style="color: #8ab4f8">remove_circle</span>
          <p class="text-base flex-grow">Quieter <span style="color: #9aa0a6">(−1 dB)</span></p>
        </div>
        <div class="flex items-center py-3 cursor-pointer" @click="$emit('level', 0)">
          <span class="material-symbols text-3xl mr-4" style="color: #8ab4f8">volume_off</span>
          <p class="text-base flex-grow">Off</p>
        </div>
        <p class="text-base mt-3" style="color: #9aa0a6">Now: {{ level > 0 ? '+' + level + ' dB' : 'Off' }}</p>
      </div>

      <!-- queue screen: the list of chapters (title set by the app) -->
      <div v-else-if="screen === 'queue'" class="flex-grow overflow-y-auto px-5 py-3">
        <p class="text-lg mb-2">{{ chaptersInQueue ? 'Chapters' : 'Queue' }}</p>
        <template v-if="chaptersInQueue">
          <div v-for="(chapter, index) in chapters" :key="chapter.title" class="flex items-center py-2.5 cursor-pointer rounded-lg px-2" :style="index === currentChapter ? 'background:#2b3a55' : ''" @click="currentChapter = index; screen = 'playing'">
            <span class="material-symbols mr-3" :style="index === currentChapter ? 'color:#8ab4f8' : 'color:transparent'">equalizer</span>
            <p class="text-base flex-grow truncate">{{ chapter.title }}</p>
            <p class="text-sm" style="color: #9aa0a6">{{ chapter.start }}</p>
          </div>
        </template>
        <div v-else class="flex items-center py-2.5 px-2 rounded-lg" style="background: #2b3a55">
          <span class="material-symbols mr-3" style="color: #8ab4f8">equalizer</span>
          <p class="text-base flex-grow truncate">Project Hail Mary.m4b</p>
        </div>
        <p v-if="!chaptersInQueue" class="text-sm mt-3" style="color: #9aa0a6">One file, so the queue has one entry.</p>
      </div>

      <!-- now playing -->
      <div v-else class="flex-grow flex px-6 py-4 min-h-0">
        <div class="flex-shrink-0 rounded-xl self-center flex items-center justify-center" :style="{ width: '190px', height: '190px', background: covers ? 'linear-gradient(135deg, #8b2c2c, #5b1a1a)' : '#3c4043' }">
          <span v-if="!covers" class="material-symbols text-6xl" style="color: #9aa0a6">music_note</span>
        </div>
        <div class="flex-grow flex flex-col justify-center pl-8 min-w-0">
          <p class="text-2xl font-medium truncate">Project Hail Mary</p>
          <p class="text-base mb-5" style="color: #9aa0a6">Andy Weir · {{ chapters[currentChapter].title }} of {{ chapters.length }}</p>

          <div class="rounded-full" style="height: 4px; background: #3c4043">
            <div class="rounded-full" style="height: 4px; background: #8ab4f8" :style="{ width: chapterProgress ? '62%' : '38%' }" />
          </div>
          <div class="flex justify-between text-xs mt-1 mb-4" style="color: #9aa0a6">
            <span>{{ chapterProgress ? '12:05' : '3:42:10' }}</span><span>{{ chapterProgress ? '-7:21' : '-5:58:20' }}</span>
          </div>

          <!-- playback buttons: standard controls in the middle, custom actions around them -->
          <div class="flex items-center justify-center space-x-2">
            <div v-for="action in actionsLeft" :key="action.id" class="aa-btn" :class="action.proposed ? 'aa-proposed' : ''">
              <span v-if="action.text" class="text-sm font-semibold">{{ action.text }}</span>
              <span v-else class="material-symbols text-2xl">{{ action.icon }}</span>
            </div>
            <div class="aa-btn" title="Previous chapter" @click="currentChapter = Math.max(0, currentChapter - 1)"><span class="material-symbols text-3xl">skip_previous</span></div>
            <div class="aa-btn aa-play" @click="playing = !playing"><span class="material-symbols text-4xl" style="color: #131417">{{ playing ? 'pause' : 'play_arrow' }}</span></div>
            <div class="aa-btn" title="Next chapter" @click="currentChapter = Math.min(chapters.length - 1, currentChapter + 1)"><span class="material-symbols text-3xl">skip_next</span></div>
            <div v-for="action in actionsRight" :key="action.id" class="aa-btn" :class="[action.proposed ? 'aa-proposed' : '', action.wide ? 'aa-wide' : '']" @click="onAction(action)">
              <span v-if="action.text" class="text-sm font-semibold" :style="action.highlight ? 'color:#8ab4f8' : ''">{{ action.text }}</span>
              <span v-else class="material-symbols text-2xl">{{ action.icon }}</span>
            </div>
          </div>
          <p v-if="boostStyle === 'plusminus' && boostEnabled" class="text-center text-xs mt-2" style="color: #9aa0a6">Boost: {{ level > 0 ? '+' + level + ' dB' : 'Off' }}</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  props: {
    /** Android Auto boost level in dB (0 - 20) */
    level: Number,
    /** volume boost master switch */
    boostEnabled: Boolean,
    /** 'toggle' | 'plusminus' | 'step' | 'list' */
    boostStyle: { type: String, default: 'toggle' },
    /** show the ideas that are not built yet */
    proposed: Boolean,
    /** the bar shows the current chapter instead of the whole book */
    chapterProgress: Boolean,
    /** the queue lists the chapters instead of the files */
    chaptersInQueue: Boolean,
    /** cover art shows up (false: what the car shows now) */
    covers: { type: Boolean, default: true }
  },
  data() {
    return {
      screen: 'browse',
      activeTab: 'continue',
      playing: true,
      boostActive: true,
      currentChapter: 11,
      chapters: Array.from({ length: 40 }, (_, i) => ({ title: 'Chapter ' + (i + 1), start: this.fmt(i * 880) })),
      books: [
        { title: 'Project Hail Mary', author: 'Andy Weir', progress: '38%', color: '#8b2c2c' },
        { title: 'The Way of Kings', author: 'Brandon Sanderson', progress: '12%', color: '#2c5f8b' },
        { title: 'Dune', author: 'Frank Herbert', progress: '71%', color: '#8b7a2c' },
        { title: 'The Hobbit', author: 'J.R.R. Tolkien', progress: '5%', color: '#3f7a4a' },
        { title: 'Circe', author: 'Madeline Miller', progress: '90%', color: '#6b3f8b' }
      ]
    }
  },
  computed: {
    tabs() {
      const tabs = [
        { id: 'continue', title: 'Continue', icon: 'headphones' },
        { id: 'recent', title: 'Recent', icon: 'schedule' },
        { id: 'libraries', title: 'Libraries', icon: 'folder' },
        { id: 'downloads', title: 'Downloads', icon: 'download_done' }
      ]
      if (this.boostEnabled && this.boostStyle === 'list') tabs.push({ id: 'boostlist', title: 'Volume boost', icon: 'volume_up', proposed: true })
      if (this.proposed) tabs.push({ id: 'series', title: 'Series', icon: 'library_books', proposed: true })
      return tabs
    },
    actionsLeft() {
      const list = [{ id: 'speed', text: '1.0x' }, { id: 'back', icon: 'replay_10' }]
      if (this.proposed) list.unshift({ id: 'sleep', icon: 'bedtime', proposed: true })
      return list
    },
    actionsRight() {
      const list = [{ id: 'fwd', icon: 'forward_30' }]
      if (this.boostEnabled) {
        const on = this.boostActive && this.level > 0
        if (this.boostStyle === 'toggle') list.push({ id: 'boost', text: on ? '+' + this.level : 'Off', highlight: on, kind: 'toggle' })
        else if (this.boostStyle === 'plusminus') list.push({ id: 'minus', icon: 'remove', kind: 'minus', proposed: true }, { id: 'plus', icon: 'add', kind: 'plus', proposed: true })
        else if (this.boostStyle === 'step') list.push({ id: 'step', text: this.level > 0 ? '+' + this.level : 'Off', highlight: this.level > 0, kind: 'step', proposed: true })
      }
      if (this.proposed) list.push({ id: 'bookmark', icon: 'bookmark_add', proposed: true })
      return list
    }
  },
  watch: {
    boostStyle() {
      if (this.activeTab === 'boostlist') this.activeTab = 'continue'
    },
    boostEnabled(on) {
      if (!on && this.activeTab === 'boostlist') this.activeTab = 'continue'
    }
  },
  methods: {
    fmt(s) {
      return `${Math.floor(s / 3600)}:${String(Math.floor((s % 3600) / 60)).padStart(2, '0')}:${String(s % 60).padStart(2, '0')}`
    },
    back() {
      this.screen = this.screen === 'queue' ? 'playing' : 'browse'
    },
    onAction(action) {
      if (action.kind === 'toggle') this.boostActive = !this.boostActive
      else if (action.kind === 'plus') this.$emit('level', Math.min(20, this.level + 1))
      else if (action.kind === 'minus') this.$emit('level', Math.max(0, this.level - 1))
      else if (action.kind === 'step') this.$emit('level', this.level >= 20 ? 0 : this.level + 1)
    }
  }
}
</script>

<style>
.aa-btn {
  width: 52px;
  height: 52px;
  border-radius: 9999px;
  background: #26282c;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  flex-shrink: 0;
}
.aa-play {
  width: 68px;
  height: 68px;
  background: #8ab4f8;
}
.aa-proposed {
  outline: 2px dashed #f2b84b;
  outline-offset: 2px;
}
</style>
