<template>
  <modals-modal v-model="show" :width="360" height="100%">
    <template #outer>
      <div class="absolute top-10 left-4 z-40 pt-1 pb-1.5" style="max-width: 80%">
        <p class="text-white text-xl truncate">{{ $strings.LabelPlaybackHistory }}</p>
      </div>
    </template>

    <div class="w-full h-full overflow-hidden absolute top-0 left-0 flex items-center justify-center" @click="show = false">
      <div class="w-full overflow-x-hidden overflow-y-auto bg-primary rounded-lg border border-fg/20 p-2" style="max-height: 80%" @click.stop>
        <p class="px-3 pt-2 pb-1 text-xs text-fg-muted">{{ $strings.MessagePlaybackHistoryHelp }}</p>
        <p v-if="!entries.length" class="px-3 py-8 text-center text-fg-muted">{{ $strings.MessagePlaybackHistoryEmpty }}</p>
        <div v-for="entry in entries" :key="entry.id" class="flex items-center px-3 py-3 rounded-md cursor-pointer" @click="$emit('select', entry)">
          <span class="material-symbols text-2xl text-fg-muted w-10 flex-shrink-0">{{ entry.icon }}</span>
          <div class="flex-grow min-w-0">
            <p class="truncate">{{ $secondsToTimestamp(entry.from) }}<span v-if="chapterTitle(entry.from)"> · {{ chapterTitle(entry.from) }}</span></p>
            <p class="text-xs text-fg-muted truncate">{{ entry.reason }} · {{ ago(entry.at) }}</p>
          </div>
          <span class="material-symbols text-fg-muted">undo</span>
        </div>
        <div v-if="entries.length" class="flex justify-end px-3 pt-1 pb-2">
          <button type="button" class="text-sm text-fg-muted" @click="$emit('clear')">{{ $strings.ButtonClear }}</button>
        </div>
      </div>
    </div>
  </modals-modal>
</template>

<script>
export default {
  props: {
    value: Boolean,
    entries: { type: Array, default: () => [] },
    chapters: { type: Array, default: () => [] }
  },
  computed: {
    show: {
      get() {
        return this.value
      },
      set(val) {
        this.$emit('input', val)
      }
    }
  },
  methods: {
    chapterTitle(time) {
      const chapter = this.chapters.find((ch) => Number(ch.start) <= time && Number(ch.end) > time)
      return chapter ? chapter.title : ''
    },
    ago(at) {
      const minutes = Math.round((Date.now() - at) / 60000)
      if (minutes < 1) return this.$strings.LabelJustNow
      if (minutes < 60) return `${minutes} min`
      const hours = Math.round(minutes / 60)
      if (hours < 24) return `${hours} h`
      return `${Math.round(hours / 24)} d`
    }
  }
}
</script>
