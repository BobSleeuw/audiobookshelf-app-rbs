<template>
  <!-- Pop-up with all options of a setting. Long lists get a search box. -->
  <div class="absolute inset-0 z-20 flex items-end bg-black/60" @click="$emit('close')">
    <div class="w-full max-h-[75%] flex flex-col bg-primary rounded-t-xl border-t border-fg/10" @click.stop>
      <div class="flex items-center px-4 h-12 flex-shrink-0">
        <p class="flex-grow font-semibold truncate">{{ title }}</p>
        <button type="button" class="material-symbols text-fg-muted" @click="$emit('close')">close</button>
      </div>

      <div v-if="options.length > 8" class="px-4 pb-2 flex-shrink-0">
        <div class="flex items-center bg-bg rounded-lg px-3 h-9">
          <span class="material-symbols text-fg-muted mr-2 text-xl">search</span>
          <input v-model="filter" type="text" :placeholder="$strings.ButtonSearch || 'Search'" class="flex-grow bg-transparent outline-none text-sm" />
        </div>
      </div>

      <div class="overflow-y-auto pb-2">
        <button v-for="option in filtered" :key="option" type="button" class="w-full flex items-center text-left px-4 py-3" :class="option === value ? 'bg-fg/10' : ''" @click="$emit('select', option)">
          <span class="flex-grow">{{ option }}</span>
          <span v-if="option === value" class="material-symbols text-success">check</span>
        </button>
        <p v-if="!filtered.length" class="px-4 py-4 text-center text-fg-muted">No matches</p>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  props: {
    title: String,
    options: Array,
    value: String
  },
  data() {
    return { filter: '' }
  },
  computed: {
    filtered() {
      const q = this.filter.trim().toLowerCase()
      return q ? this.options.filter((o) => o.toLowerCase().includes(q)) : this.options
    }
  }
}
</script>
