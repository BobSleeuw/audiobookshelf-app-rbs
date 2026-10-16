<template>
  <!-- Same markup as the rows on the original Settings page (plus a few new row types for the mockups) -->
  <div v-if="item.type === 'toggle'" class="flex items-center py-3 cursor-pointer" @click="toggle">
    <div class="flex-grow pr-4">
      <p>{{ label }}</p>
      <p v-if="item.help" class="text-xs text-fg-muted mt-0.5">{{ item.help }}</p>
    </div>
    <ui-toggle-switch :value="!!values[item.id]" class="pointer-events-none flex-shrink-0" />
  </div>

  <div v-else-if="item.type === 'select'" class="py-3">
    <div class="flex items-center">
      <p class="flex-grow pr-4">{{ label }}</p>
      <div class="flex-shrink-0" @click.stop="pick">
        <ui-text-input :value="values[item.id]" readonly append-icon="expand_more" :autofocus="false" style="width: 200px" />
      </div>
    </div>
    <p v-if="item.help" class="text-xs text-fg-muted mt-1.5">{{ item.help }}</p>
  </div>

  <div v-else-if="item.type === 'time'" class="py-3 flex items-center">
    <p class="flex-grow pr-4">{{ label }}</p>
    <ui-text-input v-model="values[item.id]" type="time" :autofocus="false" class="flex-shrink-0" style="width: 145px" />
  </div>

  <div v-else-if="item.type === 'slider'" class="py-3">
    <div class="flex items-center">
      <p class="flex-grow">{{ label }}</p>
      <p class="text-sm tabular-nums">{{ item.plain ? values[item.id] + item.unit : values[item.id] > 0 ? '+' + values[item.id] + item.unit : $strings.LabelOff }}</p>
    </div>
    <input v-model.number="values[item.id]" type="range" :min="item.min" :max="item.max" step="1" class="w-full" />
  </div>

  <!-- slider that snaps to a fixed list of values, with a dot and a label for every value -->
  <div v-else-if="item.type === 'steps'" class="py-3">
    <div class="flex items-center">
      <p class="flex-grow">{{ label }}</p>
      <p class="text-sm tabular-nums">{{ values[item.id] }}</p>
    </div>
    <p v-if="item.help" class="text-xs text-fg-muted mt-0.5">{{ item.help }}</p>

    <!-- "End of chapter" is not a length, so it is a separate choice instead of a point on the slider -->
    <div v-if="item.special" class="mt-2 mb-1">
      <ui-toggle-btns :value="isSpecial ? 'special' : 'time'" :items="[{ text: item.timeText || 'Set time', value: 'time' }, { text: item.special, value: 'special' }]" @input="setMode" />
    </div>

    <template v-if="!isSpecial">
    <input :value="stepIndex" type="range" min="0" :max="item.options.length - 1" step="1" class="w-full" @input="setStep($event.target.value)" />
    <div class="flex justify-between px-2 -mt-0.5 pointer-events-none">
      <div v-for="(option, index) in item.options" :key="option" class="w-0 flex flex-col items-center">
        <span class="w-1.5 h-1.5 rounded-full" :class="index === stepIndex ? 'bg-success' : 'bg-fg-muted'" />
        <span class="text-[10px] mt-1 whitespace-nowrap" :class="index === stepIndex ? 'text-fg' : 'text-fg-muted'">{{ item.ticks ? item.ticks[index] : option }}</span>
      </div>
    </div>
    </template>
  </div>

  <!-- start and end time on one line; tapping one opens the Android style clock picker -->
  <div v-else-if="item.type === 'timerange'" class="py-3">
    <p>{{ label }}</p>
    <p v-if="item.help" class="text-xs text-fg-muted mt-0.5">{{ item.help }}</p>
    <div class="grid items-center gap-x-3 gap-y-1 mt-2" style="grid-template-columns: 1fr auto 1fr">
      <p class="text-xs text-fg-muted">{{ $strings.LabelStartTime }}</p>
      <span />
      <p class="text-xs text-fg-muted">{{ $strings.LabelEndTime }}</p>

      <div class="cursor-pointer" @click="openClock(item.startId, $strings.LabelStartTime)">
        <ui-text-input :value="values[item.startId]" readonly append-icon="schedule" :autofocus="false" />
      </div>
      <span class="material-symbols text-fg-muted">arrow_forward</span>
      <div class="cursor-pointer" @click="openClock(item.endId, $strings.LabelEndTime)">
        <ui-text-input :value="values[item.endId]" readonly append-icon="schedule" :autofocus="false" />
      </div>
    </div>
    <settingsmock-clock-picker v-model="showClock" :time="clockTime" :title="clockTitle" @confirm="setClockTime" />
  </div>

  <!-- text with a button -->
  <div v-else-if="item.type === 'action'" class="py-3 flex items-center">
    <div class="flex-grow pr-4">
      <p>{{ label }}</p>
      <p v-if="item.help" class="text-xs text-fg-muted mt-0.5">{{ item.help }}</p>
    </div>
    <ui-btn small class="flex-shrink-0">{{ item.buttonText }}</ui-btn>
  </div>

  <!-- list of devices with an on / off toggle for each -->
  <div v-else-if="item.type === 'devices'" class="py-3">
    <p>{{ label }}</p>
    <p v-if="item.help" class="text-xs text-fg-muted mt-0.5 mb-1">{{ item.help }}</p>
    <div v-for="device in item.devices" :key="device.id" class="flex items-center py-2 cursor-pointer" @click="values[device.id] = !values[device.id]">
      <span class="material-symbols text-2xl text-fg-muted w-9 flex-shrink-0">{{ values[device.id] ? 'bluetooth_connected' : 'bluetooth' }}</span>
      <div class="flex-grow min-w-0 pr-4">
        <p class="truncate">{{ device.name }}</p>
        <p class="text-xs text-fg-muted truncate">{{ device.detail }}</p>
      </div>
      <ui-toggle-switch :value="!!values[device.id]" class="pointer-events-none flex-shrink-0" />
    </div>
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
  data() {
    return { showClock: false, clockKey: null, clockTitle: '', lastTime: null }
  },
  computed: {
    clockTime() {
      return this.clockKey ? this.values[this.clockKey] : '00:00'
    },
    /** The "special" value (End of chapter) is selected instead of a length */
    isSpecial() {
      return !!this.item.special && this.values[this.item.id] === this.item.special
    },
    stepIndex() {
      return Math.max(0, (this.item.options || []).indexOf(this.values[this.item.id]))
    },
    label() {
      return this.item.label || this.$strings[this.item.labelKey] || this.item.labelKey
    }
  },
  methods: {
    setMode(mode) {
      if (mode === 'special') {
        // Remember the length so it comes back when going back to "Set time"
        this.lastTime = this.values[this.item.id]
        this.values[this.item.id] = this.item.special
      } else {
        this.values[this.item.id] = this.lastTime || this.item.default || this.item.options[0]
      }
    },
    setStep(index) {
      this.values[this.item.id] = this.item.options[Number(index)]
    },
    openClock(key, title) {
      this.clockKey = key
      this.clockTitle = title
      this.showClock = true
    },
    setClockTime(time) {
      this.values[this.clockKey] = time
    },
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
