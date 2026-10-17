<template>
  <!-- Mockup: a book's page with tabs for chapters, bookmarks, notes and characters -->
  <div class="relative w-full h-full flex flex-col overflow-hidden bg-bg text-fg">
    <div class="flex items-center h-14 px-3 bg-primary flex-shrink-0">
      <span class="material-symbols text-2xl mr-2">arrow_back</span>
      <p class="flex-grow truncate">Project Hail Mary</p>
    </div>

    <!-- tabs -->
    <div class="flex flex-shrink-0 border-b border-fg/10 text-sm">
      <button v-for="t in tabs" :key="t.id" type="button" class="flex-1 py-3" :class="tab === t.id ? 'text-fg border-b-2 border-success' : 'text-fg-muted'" @click="tab = t.id">{{ t.title }}</button>
    </div>

    <div class="flex-grow overflow-y-auto px-4 py-3">
      <!-- notes -->
      <template v-if="tab === 'notes'">
        <div v-for="note in notes" :key="note.id" class="py-3 border-b border-fg/10">
          <div class="flex items-center text-xs text-fg-muted mb-1">
            <span class="material-symbols text-base mr-1 cursor-pointer" @click="$emit('jump', note.at)">play_circle</span>
            <span>{{ fmt(note.at) }} · {{ chapterName(note.at) }}</span>
            <span class="flex-grow" />
            <span class="material-symbols text-base cursor-pointer" @click="notes = notes.filter((n) => n.id !== note.id)">delete</span>
          </div>
          <p class="text-sm">{{ note.text }}</p>
        </div>
        <p v-if="!notes.length" class="py-8 text-center text-fg-muted">No notes yet.</p>
        <div class="mt-4">
          <ui-text-input v-model="newNote" placeholder="Write a note at the current position" :autofocus="false" @keyup.enter="addNote" />
          <ui-btn small class="mt-2" :disabled="!newNote.trim()" @click="addNote">Add note at {{ fmt(position) }}</ui-btn>
        </div>
      </template>

      <!-- characters -->
      <template v-else-if="tab === 'characters'">
        <div class="flex items-center py-2 cursor-pointer" @click="hideSpoilers = !hideSpoilers">
          <p class="flex-grow text-sm">Hide characters I have not met yet</p>
          <ui-toggle-switch :value="hideSpoilers" class="pointer-events-none" />
        </div>

        <div v-for="c in visibleCharacters" :key="c.id" class="py-3 border-b border-fg/10">
          <div class="flex items-center">
            <div class="w-10 h-10 rounded-full flex items-center justify-center mr-3 flex-shrink-0" :style="{ background: c.color }">{{ c.name.charAt(0) }}</div>
            <div class="flex-grow min-w-0">
              <p class="truncate">{{ c.name }}</p>
              <p class="text-xs text-fg-muted">First met at {{ fmt(c.at) }}</p>
            </div>
            <span class="material-symbols text-fg-muted cursor-pointer" @click="$emit('jump', c.at)">play_circle</span>
          </div>
          <p class="text-sm text-fg-muted mt-1 pl-13" style="padding-left: 3.25rem">{{ c.description }}</p>
        </div>
        <p v-if="!visibleCharacters.length" class="py-8 text-center text-fg-muted">No characters yet.</p>
        <p v-if="hiddenCount" class="text-xs text-fg-muted text-center py-3">{{ hiddenCount }} more characters appear as you listen.</p>

        <div class="mt-4 border-t border-fg/10 pt-3">
          <ui-text-input v-model="newName" placeholder="Name" :autofocus="false" class="mb-2" />
          <ui-text-input v-model="newDescription" placeholder="Who is this?" :autofocus="false" />
          <ui-btn small class="mt-2" :disabled="!newName.trim()" @click="addCharacter">Add character (met at {{ fmt(position) }})</ui-btn>
        </div>
      </template>

      <!-- bookmarks (exists in the app today) -->
      <template v-else-if="tab === 'bookmarks'">
        <div v-for="b in bookmarks" :key="b.at" class="flex items-center py-3 border-b border-fg/10">
          <span class="material-symbols text-fg-muted mr-3">bookmark</span>
          <div class="flex-grow">
            <p class="text-sm">{{ b.title }}</p>
            <p class="text-xs text-fg-muted">{{ fmt(b.at) }}</p>
          </div>
        </div>
        <p class="text-xs text-fg-muted mt-4">Bookmarks already exist in the app and are saved on the server. They only have a short title.</p>
      </template>

      <!-- chapters -->
      <template v-else>
        <div v-for="c in chapters" :key="c.start" class="flex items-center py-3 border-b border-fg/10 text-sm">
          <p class="flex-grow">{{ c.title }}</p>
          <p class="text-fg-muted">{{ fmt(c.start) }}</p>
        </div>
      </template>
    </div>
  </div>
</template>

<script>
export default {
  props: {
    position: { type: Number, default: 13330 }
  },
  data() {
    return {
      tab: 'characters',
      hideSpoilers: true,
      newNote: '',
      newName: '',
      newDescription: '',
      nextId: 100,
      chapters: [
        { title: 'Chapter 1', start: 0 },
        { title: 'Chapter 2', start: 2300 },
        { title: 'Chapter 3', start: 5200 },
        { title: 'Chapter 4', start: 8100 },
        { title: 'Chapter 5', start: 10900 },
        { title: 'Chapter 6', start: 12730 },
        { title: 'Chapter 7', start: 15500 }
      ],
      bookmarks: [
        { title: 'Funny bit about the coffee', at: 1830 },
        { title: 'Check this later', at: 9660 }
      ],
      notes: [
        { id: 1, at: 2900, text: 'Ryland has lost his memory. Everything we learn about him comes from flashbacks.' },
        { id: 2, at: 9100, text: 'The signal is repeating every 3 hours. Probably important.' }
      ],
      characters: [
        { id: 1, name: 'Ryland Grace', color: '#8b2c2c', at: 60, description: 'Wakes up alone on a spaceship with no memory. A teacher before the mission.' },
        { id: 2, name: 'Eva Stratt', color: '#2c5f8b', at: 4300, description: 'Leads the project that built the ship. Very hard to say no to.' },
        { id: 3, name: 'Rocky', color: '#3f7a4a', at: 18800, description: 'Someone Ryland meets much later in the story.' },
        { id: 4, name: 'Yao', color: '#8b7a2c', at: 6200, description: 'Commander of the mission.' }
      ]
    }
  },
  computed: {
    tabs() {
      return [
        { id: 'chapters', title: 'Chapters' },
        { id: 'bookmarks', title: 'Bookmarks' },
        { id: 'notes', title: 'Notes' },
        { id: 'characters', title: 'Characters' }
      ]
    },
    visibleCharacters() {
      return this.characters.filter((c) => !this.hideSpoilers || c.at <= this.position).sort((a, b) => a.at - b.at)
    },
    hiddenCount() {
      return this.characters.length - this.visibleCharacters.length
    }
  },
  methods: {
    fmt(s) {
      s = Math.max(0, Math.round(s))
      return `${Math.floor(s / 3600)}:${String(Math.floor((s % 3600) / 60)).padStart(2, '0')}:${String(s % 60).padStart(2, '0')}`
    },
    chapterName(s) {
      let name = this.chapters[0].title
      this.chapters.forEach((c) => {
        if (s >= c.start) name = c.title
      })
      return name
    },
    addNote() {
      if (!this.newNote.trim()) return
      this.notes = [...this.notes, { id: this.nextId++, at: this.position, text: this.newNote.trim() }].sort((a, b) => a.at - b.at)
      this.newNote = ''
    },
    addCharacter() {
      if (!this.newName.trim()) return
      this.characters.push({ id: this.nextId++, name: this.newName.trim(), color: '#6b3f8b', at: this.position, description: this.newDescription.trim() || 'No description yet.' })
      this.newName = ''
      this.newDescription = ''
    }
  }
}
</script>
