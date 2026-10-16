<template>
  <!-- Approximate Android Auto media screen (what Google draws from our browse tree + playback buttons) -->
  <div class="aa-screen flex w-full h-full overflow-hidden text-white select-none" style="background: #131417; font-family: 'Google Sans', Roboto, sans-serif">
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
      <!-- top bar: app name + tabs (browse root items) -->
      <div class="flex items-center px-5 flex-shrink-0" style="height: 56px; border-bottom: 1px solid #26282c">
        <span class="material-symbols text-2xl mr-3" style="color: #9aa0a6" v-if="screen !== 'browse'" @click="screen = 'browse'">arrow_back</span>
        <p class="text-lg font-medium mr-6">ABS-RBS</p>
        <div v-if="screen === 'browse'" class="flex space-x-1">
          <div v-for="tab in tabs" :key="tab.id" class="px-4 py-1.5 rounded-full cursor-pointer text-sm" :style="activeTab === tab.id ? 'background:#2b3a55;color:#8ab4f8' : 'color:#c4c7cc'" @click="activeTab = tab.id">
            <span class="material-symbols align-middle mr-1" style="font-size: 18px">{{ tab.icon }}</span>{{ tab.title }}
          </div>
        </div>
        <p v-else-if="screen === 'playing'" class="text-sm" style="color: #9aa0a6">Now playing</p>
      </div>

      <!-- browse: list of books / libraries / boost levels -->
      <div v-if="screen === 'browse'" class="flex-grow overflow-y-auto px-5 py-3">
        <template>
          <div v-for="book in books" :key="book.title" class="flex items-center py-2.5 cursor-pointer" @click="screen = 'playing'">
            <div class="rounded-lg mr-4 flex-shrink-0" :style="{ width: '52px', height: '52px', background: book.color }" />
            <div class="min-w-0 flex-grow">
              <p class="text-base truncate">{{ book.title }}</p>
              <p class="text-sm truncate" style="color: #9aa0a6">{{ book.author }} · {{ book.progress }}</p>
            </div>
          </div>
        </template>
      </div>

      <!-- now playing -->
      <div v-else class="flex-grow flex px-6 py-4 min-h-0">
        <div class="flex-shrink-0 rounded-xl self-center" style="width: 190px; height: 190px; background: linear-gradient(135deg, #8b2c2c, #5b1a1a)" />
        <div class="flex-grow flex flex-col justify-center pl-8 min-w-0">
          <p class="text-2xl font-medium truncate">Project Hail Mary</p>
          <p class="text-base mb-5" style="color: #9aa0a6">Andy Weir · Chapter 12 of 40</p>

          <div class="rounded-full" style="height: 4px; background: #3c4043">
            <div class="rounded-full" style="height: 4px; background: #8ab4f8" :style="{ width: chapterProgress ? '62%' : '38%' }" />
          </div>
          <div class="flex justify-between text-xs mt-1 mb-4" style="color: #9aa0a6">
            <span>{{ chapterProgress ? '12:05' : '3:42:10' }}</span><span>{{ chapterProgress ? '-7:21' : '-5:58:20' }}</span>
          </div>

          <!-- playback buttons: standard controls in the middle, custom actions around them -->
          <div class="flex items-center justify-center space-x-3">
            <div v-for="action in actionsLeft" :key="action.id" class="aa-btn" :class="action.proposed ? 'aa-proposed' : ''" @click="onAction(action)">
              <span v-if="action.text" class="text-sm font-semibold" :style="action.highlight ? 'color:#8ab4f8' : ''">{{ action.text }}</span>
              <span v-else class="material-symbols text-2xl">{{ action.icon }}</span>
            </div>
            <div class="aa-btn" title="Previous chapter"><span class="material-symbols text-3xl">skip_previous</span></div>
            <div class="aa-btn aa-play"><span class="material-symbols text-4xl" style="color: #131417">{{ playing ? 'pause' : 'play_arrow' }}</span></div>
            <div class="aa-btn" title="Next chapter"><span class="material-symbols text-3xl">skip_next</span></div>
            <div v-for="action in actionsRight" :key="action.id" class="aa-btn" :class="action.proposed ? 'aa-proposed' : ''" @click="onAction(action)">
              <span v-if="action.text" class="text-sm font-semibold" :style="action.highlight ? 'color:#8ab4f8' : ''">{{ action.text }}</span>
              <span v-else class="material-symbols text-2xl">{{ action.icon }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  props: {
    /** Android Auto boost level in dB */
    level: Number,
    /** volume boost master switch */
    boostEnabled: Boolean,
    /** show the ideas that are not built yet */
    proposed: Boolean,
    /** the bar shows the current chapter instead of the whole book */
    chapterProgress: Boolean
  },
  data() {
    return {
      screen: 'browse',
      activeTab: 'continue',
      playing: true,
      boostActive: true,
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
      if (this.proposed) tabs.push({ id: 'series', title: 'Series', icon: 'library_books', proposed: true })
      return tabs
    },
    boostAction() {
      const on = this.boostActive && this.level > 0
      return { id: 'boost', text: on ? '+' + this.level : 'Off', highlight: on, kind: 'boost' }
    },
    actionsLeft() {
      const list = [{ id: 'speed', text: '1.0x', kind: 'speed' }, { id: 'back', icon: 'replay_10', kind: 'jump' }]
      if (this.proposed) list.unshift({ id: 'sleep', icon: 'bedtime', proposed: true, kind: 'idea' })
      return list
    },
    actionsRight() {
      const list = [{ id: 'fwd', icon: 'forward_30', kind: 'jump' }]
      if (this.boostEnabled) list.push(this.boostAction)
      if (this.proposed) list.push({ id: 'bookmark', icon: 'bookmark_add', proposed: true, kind: 'idea' })
      return list
    }
  },
  watch: {
    boostEnabled(on) {
      if (!on && this.activeTab === 'boost') this.activeTab = 'continue'
    }
  },
  methods: {
    onAction(action) {
      if (action.kind === 'boost') this.boostActive = !this.boostActive
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
