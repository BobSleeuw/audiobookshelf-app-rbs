<template>
  <!-- Same markup as the rows on the original Settings page -->
  <div v-if="item.type === 'toggle'" class="flex items-center py-3 cursor-pointer" @click="toggle">
    <p class="flex-grow pr-4">{{ label }}</p>
    <ui-toggle-switch :value="!!values[item.id]" class="pointer-events-none flex-shrink-0" />
  </div>

  <div v-else-if="item.type === 'select'" class="py-3 flex items-center">
    <p class="flex-grow pr-4">{{ label }}</p>
    <div class="flex-shrink-0" @click.stop="pick">
      <ui-text-input :value="values[item.id]" readonly append-icon="expand_more" :autofocus="false" style="width: 200px" />
    </div>
  </div>

  <div v-else-if="item.type === 'time'" class="py-3 flex items-center">
    <p class="flex-grow pr-4">{{ label }}</p>
    <ui-text-input v-model="values[item.id]" type="time" :autofocus="false" class="flex-shrink-0" style="width: 145px" />
  </div>

  <div v-else-if="item.type === 'slider'" class="py-3">
    <div class="flex items-center">
      <p class="flex-grow">{{ label }}</p>
      <p class="text-sm tabular-nums">{{ values[item.id] > 0 ? '+' + values[item.id] + item.unit : $strings.LabelOff }}</p>
    </div>
    <input v-model.number="values[item.id]" type="range" :min="item.min" :max="item.max" step="1" class="w-full" />
  </div>

  <div v-else-if="item.type === 'link'" class="py-3 flex items-center">
    <span class="material-symbols text-2xl text-fg-muted">{{ item.icon || 'chevron_right' }}</span>
    <p class="pl-4 flex-grow">{{ label }}</p>
    <span class="material-symbols text-fg-muted">chevron_right</span>
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
    toggle() {
      this.values[this.item.id] = !this.values[this.item.id]
    },
    pick() {
      if (this.$listeners.pick) {
        this.$emit('pick', this.item)
      } else {
        // Mockups without a pop-up: cycle through the options
        const options = this.item.options
        this.values[this.item.id] = options[(options.indexOf(this.values[this.item.id]) + 1) % options.length]
      }
    }
  }
}
</script>
