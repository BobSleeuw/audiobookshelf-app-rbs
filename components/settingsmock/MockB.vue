<template>
  <!-- Mockup B: everything on one page, but each category is a collapsible section (accordion) -->
  <div class="w-full h-full flex flex-col overflow-hidden bg-bg text-fg">
    <settingsmock-header title="Settings" />

    <div class="flex-grow overflow-y-auto p-3 space-y-2">
      <div v-for="group in groups" :key="group.id" class="bg-primary rounded-lg overflow-hidden">
        <button type="button" class="w-full flex items-center text-left p-3" @click="toggle(group.id)">
          <span class="material-symbols text-2xl text-fg-muted w-10">{{ group.icon }}</span>
          <p class="flex-grow font-semibold">{{ group.title }}</p>
          <span class="text-xs text-fg-muted mr-2">{{ visible(group).length }}</span>
          <span class="material-symbols text-fg-muted transition-transform" :class="isOpen(group.id) ? 'rotate-180' : ''">expand_more</span>
        </button>
        <div v-if="isOpen(group.id)" class="px-4 border-t border-fg/10">
          <div v-for="item in visible(group)" :key="item.id" class="border-b border-fg/10 last:border-0">
            <settingsmock-row :item="item" :values="values" />
          </div>
        </div>
      </div>

      <div class="flex justify-center pt-2">
        <button type="button" class="text-sm text-fg-muted" @click="open = open.length ? [] : groups.map((g) => g.id)">{{ open.length ? 'Collapse all' : 'Expand all' }}</button>
      </div>
    </div>
  </div>
</template>

<script>
import { GROUPS, createValues, visibleItems } from '@/mocks/settingsMock'

export default {
  data() {
    return { groups: GROUPS, values: createValues(), open: ['playback'] }
  },
  methods: {
    visible(group) {
      return visibleItems(group, this.values)
    },
    isOpen(id) {
      return this.open.includes(id)
    },
    toggle(id) {
      this.open = this.isOpen(id) ? this.open.filter((o) => o !== id) : [...this.open, id]
    }
  }
}
</script>
