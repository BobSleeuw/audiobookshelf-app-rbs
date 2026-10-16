<template>
  <div class="flex items-center py-3 min-h-[3.25rem]" @click="onClick">
    <div class="flex-grow min-w-0 pr-3">
      <p class="truncate">{{ label }}</p>
      <p v-if="item.type === 'link'" class="text-xs text-fg-muted truncate">{{ item.valueText }}</p>
    </div>

    <ui-toggle-switch v-if="item.type === 'toggle'" :value="!!values[item.id]" class="pointer-events-none" />

    <div v-else-if="item.type === 'slider'" class="flex items-center" @click.stop>
      <input v-model.number="values[item.id]" type="range" :min="item.min" :max="item.max" step="1" style="width: 7rem" />
      <p class="text-sm tabular-nums w-14 text-right">{{ values[item.id] > 0 ? '+' + values[item.id] + item.unit : 'Off' }}</p>
    </div>

    <span v-else-if="item.type === 'select' || item.type === 'time'" class="text-sm text-fg-muted flex items-center">
      {{ values[item.id] }}<span class="material-symbols text-lg ml-1">{{ item.type === 'time' ? 'schedule' : 'expand_more' }}</span>
    </span>

    <span v-else-if="item.type === 'link'" class="material-symbols text-fg-muted">chevron_right</span>
  </div>
</template>

<script>
export default {
  props: {
    item: Object,
    values: Object
  },
  computed: {
    label() {
      return this.$strings[this.item.labelKey] || this.item.labelKey
    }
  },
  methods: {
    onClick() {
      const item = this.item
      if (item.type === 'toggle') {
        this.values[item.id] = !this.values[item.id]
      } else if (item.type === 'select') {
        // Mockup only: cycle through the options instead of opening a menu
        const index = item.options.indexOf(this.values[item.id])
        this.values[item.id] = item.options[(index + 1) % item.options.length]
      }
    }
  }
}
</script>
