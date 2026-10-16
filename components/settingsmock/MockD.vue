<template>
  <!-- Mockup D: search + quick tiles on top, then category cards that show the main settings and expand for the rest -->
  <div class="w-full h-full flex flex-col overflow-hidden bg-bg text-fg">
    <settingsmock-header title="Settings" />

    <div class="flex-grow overflow-y-auto p-3">
      <div class="flex items-center bg-primary rounded-lg px-3 h-10 mb-3">
        <span class="material-symbols text-fg-muted mr-2">search</span>
        <input v-model="query" type="text" placeholder="Search settings" class="flex-grow bg-transparent outline-none" />
        <button v-if="query" type="button" class="material-symbols text-fg-muted" @click="query = ''">close</button>
      </div>

      <!-- search results -->
      <div v-if="query" class="bg-primary rounded-lg px-4">
        <p v-if="!results.length" class="py-4 text-center text-fg-muted">No settings found</p>
        <div v-for="result in results" :key="result.item.id" class="border-b border-fg/10 last:border-0">
          <settingsmock-row :item="result.item" :values="values" />
          <p class="text-xs text-fg-muted -mt-2 pb-2">{{ result.group.title }}</p>
        </div>
      </div>

      <template v-else>
        <!-- quick settings -->
        <p class="uppercase text-xs font-semibold text-fg-muted mb-2">Quick settings</p>
        <div class="grid grid-cols-3 gap-2 mb-5">
          <button v-for="tile in tiles" :key="tile.id" type="button" class="rounded-lg p-2 text-left h-20 flex flex-col justify-between" :class="isOn(tile) ? 'bg-success/20' : 'bg-primary'" @click="tapTile(tile)">
            <span class="material-symbols text-xl" :class="isOn(tile) ? 'text-success' : 'text-fg-muted'">{{ tile.icon }}</span>
            <span class="text-[11px] leading-tight">{{ tile.label }}<br /><span class="text-fg-muted">{{ tile.value }}</span></span>
          </button>
        </div>

        <!-- categories -->
        <div v-for="group in groups" :key="group.id" class="bg-primary rounded-lg px-4 mb-3">
          <div class="flex items-center pt-3 pb-1">
            <span class="material-symbols text-fg-muted mr-2">{{ group.icon }}</span>
            <p class="font-semibold flex-grow">{{ group.title }}</p>
          </div>
          <div v-for="item in shown(group)" :key="item.id" class="border-b border-fg/10 last:border-0">
            <settingsmock-row :item="item" :values="values" />
          </div>
          <button v-if="visible(group).length > 3" type="button" class="w-full text-sm text-fg-muted py-3 border-t border-fg/10" @click="toggle(group.id)">
            {{ expanded.includes(group.id) ? 'Show less' : `Show all ${visible(group).length}` }}
          </button>
        </div>
      </template>
    </div>
  </div>
</template>

<script>
import { GROUPS, QUICK_TILES, createValues, findItem, visibleItems } from '@/mocks/settingsMock'

export default {
  data() {
    return { groups: GROUPS, values: createValues(), query: '', expanded: [] }
  },
  computed: {
    results() {
      const q = this.query.trim().toLowerCase()
      const found = []
      this.groups.forEach((group) => {
        visibleItems(group, this.values).forEach((item) => {
          const label = (this.$strings[item.labelKey] || '').toLowerCase()
          if (label.includes(q) || group.title.toLowerCase().includes(q)) found.push({ group, item })
        })
      })
      return found
    },
    tiles() {
      return QUICK_TILES.map((t) => {
        const item = findItem(t.id)
        const v = this.values[t.id]
        let value = ''
        if (item.type === 'toggle') value = v ? 'On' : 'Off'
        else if (item.type === 'slider') value = v > 0 ? `+${v} dB` : 'Off'
        else if (item.type === 'link') value = 'On'
        else value = v
        return { ...t, item, label: (this.$strings[item.labelKey] || '').replace(/^Use /, '').replace(/^./, (c) => c.toUpperCase()), value }
      })
    }
  },
  methods: {
    visible(group) {
      return visibleItems(group, this.values)
    },
    shown(group) {
      const items = this.visible(group)
      return this.expanded.includes(group.id) ? items : items.slice(0, 3)
    },
    toggle(id) {
      this.expanded = this.expanded.includes(id) ? this.expanded.filter((e) => e !== id) : [...this.expanded, id]
    },
    isOn(tile) {
      const v = this.values[tile.id]
      return tile.item.type === 'toggle' ? !!v : tile.item.type === 'slider' ? v > 0 : tile.item.type === 'link'
    },
    tapTile(tile) {
      const item = tile.item
      const v = this.values[item.id]
      if (item.type === 'toggle') this.values[item.id] = !v
      else if (item.type === 'select') this.values[item.id] = item.options[(item.options.indexOf(v) + 1) % item.options.length]
      else if (item.type === 'slider') this.values[item.id] = v >= 12 ? 0 : v + 3
    }
  }
}
</script>
