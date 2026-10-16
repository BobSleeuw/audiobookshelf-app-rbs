<template>
  <!-- Mockup A: categories (with the names of the settings inside) that open one page per category, with search.
       Rows, select boxes and pop-ups are the original app's own components and markup. -->
  <div class="relative w-full h-full overflow-y-auto px-4 py-8 bg-bg text-fg">
    <!-- category list -->
    <template v-if="!current">
      <p class="uppercase text-xs font-semibold text-fg-muted mb-2">{{ $strings.HeaderSettings }}</p>
      <ui-text-input v-model="query" prepend-icon="search" clearable :autofocus="false" placeholder="Search settings" class="mb-4" />

      <!-- search results -->
      <template v-if="query.trim()">
        <p v-if="!results.length" class="py-6 text-center text-fg-muted">No settings found</p>
        <div v-for="result in results" :key="result.item.id" class="flex items-center py-3 cursor-pointer" @click="openResult(result)">
          <span class="material-symbols text-2xl text-fg-muted w-10">{{ result.group.icon }}</span>
          <div class="flex-grow min-w-0">
            <p class="truncate">{{ result.item.short }}</p>
            <p class="text-xs text-fg-muted truncate">{{ result.group.title }}</p>
          </div>
          <span class="material-symbols text-fg-muted">chevron_right</span>
        </div>
      </template>

      <!-- categories -->
      <template v-else>
        <div v-for="group in groups" :key="group.id" class="flex items-center bg-primary rounded-lg p-3 mb-2 cursor-pointer" @click="openSection(group)">
          <span class="material-symbols text-2xl text-fg-muted w-10 flex-shrink-0">{{ group.icon }}</span>
          <div class="flex-grow min-w-0 pr-2">
            <p>{{ group.title }}</p>
            <p class="text-xs text-fg-muted">{{ names(group) }}</p>
          </div>
          <span class="material-symbols text-fg-muted">chevron_right</span>
        </div>
      </template>
    </template>

    <!-- one category -->
    <div v-else ref="section">
      <div class="flex items-center mb-2 cursor-pointer text-fg-muted" @click="closeSection">
        <span class="material-symbols text-xl mr-1">arrow_back</span>
        <p class="uppercase text-xs font-semibold">{{ current.title }}</p>
      </div>
      <div v-for="item in items" :key="item.id" :data-item="item.id" class="rounded transition-colors duration-700" :class="highlightId === item.id ? 'bg-success bg-opacity-20' : ''">
        <settingsmock-row :item="item" :values="values" @pick="openPicker" />
      </div>
    </div>

    <!-- choice pop-ups: the app's own dialog, and a theme picker with previews -->
    <modals-dialog v-model="showDialog" :items="dialogOptions" :selected="dialogSelected" @action="choose" />
    <settingsmock-theme-picker v-model="showThemePicker" :selected="themeValue" @action="chooseTheme" />
  </div>
</template>

<script>
import { GROUPS, createValues, visibleItems } from '@/mocks/settingsMock'

export default {
  data() {
    return { groups: GROUPS, values: createValues(), current: null, query: '', highlightId: null, pickerItem: null, showDialog: false, showThemePicker: false }
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
    },
    dialogOptions() {
      return this.pickerItem ? this.pickerItem.options : []
    },
    dialogSelected() {
      return this.pickerItem ? this.values[this.pickerItem.id] : ''
    },
    /** 'black' | 'dark' | 'light' */
    themeValue() {
      return String(this.values.theme).toLowerCase()
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
    openPicker(item) {
      this.pickerItem = item
      if (item.id === 'theme') this.showThemePicker = true
      else this.showDialog = true
    },
    choose(option) {
      this.values[this.pickerItem.id] = option
      this.showDialog = false
    },
    chooseTheme(theme) {
      this.values.theme = theme.charAt(0).toUpperCase() + theme.slice(1)
      this.showThemePicker = false
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
