<template>
  <!-- Mockup A: categories list (names of the settings inside, no values) that opens one page per category, with search -->
  <div class="w-full h-full flex flex-col overflow-hidden bg-bg text-fg">
    <settingsmock-header :title="current ? current.title : 'Settings'" :back="!!current" @back="closeSection" />

    <!-- search -->
    <div v-if="!current" class="flex-shrink-0 px-3 pt-3">
      <div class="flex items-center bg-primary rounded-lg px-3 h-10">
        <span class="material-symbols text-fg-muted mr-2">search</span>
        <input v-model="query" type="text" placeholder="Search settings" class="flex-grow bg-transparent outline-none" />
        <button v-if="query" type="button" class="material-symbols text-fg-muted" @click="query = ''">close</button>
      </div>
    </div>

    <!-- search results -->
    <div v-if="!current && query.trim()" class="flex-grow overflow-y-auto p-3">
      <p v-if="!results.length" class="py-8 text-center text-fg-muted">No settings found</p>
      <div v-else class="bg-primary rounded-lg">
        <button v-for="result in results" :key="result.item.id" type="button" class="w-full flex items-center text-left px-3 py-3 border-b border-fg/10 last:border-0" @click="openResult(result)">
          <span class="material-symbols text-fg-muted mr-3">{{ result.group.icon }}</span>
          <div class="flex-grow min-w-0">
            <p class="truncate">{{ result.item.short }}</p>
            <p class="text-xs text-fg-muted truncate">{{ result.group.title }}</p>
          </div>
          <span class="material-symbols text-fg-muted">chevron_right</span>
        </button>
      </div>
    </div>

    <!-- category list -->
    <div v-else-if="!current" class="flex-grow overflow-y-auto p-3 space-y-2">
      <button v-for="group in groups" :key="group.id" type="button" class="w-full flex items-center text-left bg-primary rounded-lg p-3" @click="openSection(group)">
        <span class="material-symbols text-2xl text-fg-muted w-10 flex-shrink-0">{{ group.icon }}</span>
        <div class="flex-grow min-w-0 pr-2">
          <p class="font-semibold">{{ group.title }}</p>
          <p class="text-xs text-fg-muted names">{{ names(group) }}</p>
        </div>
        <span class="material-symbols text-fg-muted">chevron_right</span>
      </button>
    </div>

    <!-- one category -->
    <div v-else ref="section" class="flex-grow overflow-y-auto px-4">
      <div v-for="item in items" :key="item.id" :data-item="item.id" class="border-b border-fg/10 last:border-0 transition-colors duration-700" :class="highlightId === item.id ? 'bg-success/20' : ''">
        <settingsmock-row :item="item" :values="values" />
      </div>
    </div>
  </div>
</template>

<script>
import { GROUPS, createValues, visibleItems } from '@/mocks/settingsMock'

export default {
  data() {
    return { groups: GROUPS, values: createValues(), current: null, query: '', highlightId: null }
  },
  computed: {
    items() {
      return this.current ? visibleItems(this.current, this.values) : []
    },
    results() {
      const q = this.query.trim().toLowerCase()
      const found = []
      this.groups.forEach((group) => {
        group.items.forEach((item) => {
          const text = `${item.short} ${this.$strings[item.labelKey] || ''} ${group.title}`.toLowerCase()
          if (text.includes(q)) found.push({ group, item })
        })
      })
      return found
    }
  },
  methods: {
    /** Names of the main settings in a category (settings that only show under another one are left out) */
    names(group) {
      return group.items
        .filter((i) => !i.showIf && !i.showIfNot)
        .map((i) => i.short)
        .join(' · ')
    },
    openSection(group, focusId = null) {
      this.current = group
      this.query = ''
      this.highlightId = null
      this.$nextTick(() => {
        if (!focusId) return
        const el = this.$refs.section && this.$refs.section.querySelector(`[data-item="${focusId}"]`)
        if (el) el.scrollIntoView({ block: 'center' })
        this.highlightId = focusId
        setTimeout(() => (this.highlightId = null), 1800)
      })
    },
    closeSection() {
      this.current = null
      this.highlightId = null
    },
    /** Open the category of a search result and jump to the setting (a setting that only shows under another one jumps to that one) */
    openResult({ group, item }) {
      this.openSection(group, item.showIf || item.id)
    }
  }
}
</script>
