<template>
  <!-- Mockup A: categories list that opens one page per category (drill-down) -->
  <div class="w-full h-full flex flex-col overflow-hidden bg-bg text-fg">
    <settingsmock-header :title="current ? current.title : 'Settings'" :back="!!current" @back="current = null" />

    <div v-if="!current" class="flex-grow overflow-y-auto p-3 space-y-2">
      <button v-for="group in groups" :key="group.id" type="button" class="w-full flex items-center text-left bg-primary rounded-lg p-3" @click="current = group">
        <span class="material-symbols text-2xl text-fg-muted w-10">{{ group.icon }}</span>
        <div class="flex-grow min-w-0">
          <p class="font-semibold">{{ group.title }}</p>
          <p class="text-xs text-fg-muted truncate">{{ group.summary(values) }}</p>
        </div>
        <span class="material-symbols text-fg-muted">chevron_right</span>
      </button>
    </div>

    <div v-else class="flex-grow overflow-y-auto px-4">
      <div v-for="item in items" :key="item.id" class="border-b border-fg/10 last:border-0">
        <settingsmock-row :item="item" :values="values" />
      </div>
    </div>
  </div>
</template>

<script>
import { GROUPS, createValues, visibleItems } from '@/mocks/settingsMock'

export default {
  data() {
    return { groups: GROUPS, values: createValues(), current: null }
  },
  computed: {
    items() {
      return this.current ? visibleItems(this.current, this.values) : []
    }
  }
}
</script>
