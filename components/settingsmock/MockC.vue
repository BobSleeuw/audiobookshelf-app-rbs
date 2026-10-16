<template>
  <!-- Mockup C: tabs under the title; one category visible at a time -->
  <div class="w-full h-full flex flex-col overflow-hidden bg-bg text-fg">
    <settingsmock-header title="Settings" />

    <div class="flex bg-primary border-b border-fg/10 flex-shrink-0">
      <button v-for="group in groups" :key="group.id" type="button" class="flex-1 min-w-0 px-1 flex flex-col items-center py-2 border-b-2" :class="group.id === currentId ? 'border-success text-fg' : 'border-transparent text-fg-muted'" @click="currentId = group.id">
        <span class="material-symbols text-2xl">{{ group.icon }}</span>
        <span class="text-[11px] mt-0.5 w-full text-center truncate">{{ group.shortTitle || group.title }}</span>
      </button>
    </div>

    <div class="flex-grow overflow-y-auto px-4">
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
    return { groups: GROUPS, values: createValues(), currentId: 'appearance' }
  },
  computed: {
    items() {
      const group = this.groups.find((g) => g.id === this.currentId)
      return visibleItems(group, this.values)
    }
  }
}
</script>
