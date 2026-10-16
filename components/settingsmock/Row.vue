<template>
  <!-- Same markup as the rows on the original Settings page (plus a few new row types for the mockups) -->
  <div v-if="item.type === 'toggle'" class="flex items-center py-3 cursor-pointer" @click="toggle">
    <div class="flex-grow pr-4">
      <p>{{ label }}</p>
      <p v-if="item.help" class="text-xs text-fg-muted mt-0.5">{{ item.help }}</p>
    </div>
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

  <!-- slider that snaps to a fixed list of values, with a dot and a label for every value -->
  <div v-else-if="item.type === 'steps'" class="py-3">
    <div class="flex items-center">
      <p class="flex-grow">{{ label }}</p>
      <p class="text-sm tabular-nums">{{ values[item.id] }}</p>
    </div>
    <p v-if="item.help" class="text-xs text-fg-muted mt-0.5">{{ item.help }}</p>
    <input :value="stepIndex" type="range" min="0" :max="item.options.length - 1" step="1" class="w-full" @input="setStep($event.target.value)" />
    <div class="flex justify-between px-2 -mt-0.5 pointer-events-none">
      <div v-for="(option, index) in item.options" :key="option" class="w-0 flex flex-col items-center">
        <span class="w-1.5 h-1.5 rounded-full" :class="index === stepIndex ? 'bg-success' : 'bg-fg-muted'" />
        <span class="text-[10px] mt-1 whitespace-nowrap" :class="index === stepIndex ? 'text-fg' : 'text-fg-muted'">{{ item.ticks ? item.ticks[index] : option }}</span>
      </div>
    </div>
  </div>

  <!-- start and end time on one line, with a two-handle slider over the night (18:00 to 12:00) -->
  <div v-else-if="item.type === 'timerange'" class="py-3">
    <p>{{ label }}</p>
    <p v-if="item.help" class="text-xs text-fg-muted mt-0.5">{{ item.help }}</p>
    <div class="flex items-end space-x-3 mt-2">
      <div class="flex-1">
        <p class="text-xs text-fg-muted mb-1">{{ $strings.LabelStartTime }}</p>
        <ui-text-input v-model="values[item.startId]" type="time" :autofocus="false" />
      </div>
      <span class="material-symbols text-fg-muted pb-2">arrow_forward</span>
      <div class="flex-1">
        <p class="text-xs text-fg-muted mb-1">{{ $strings.LabelEndTime }}</p>
        <ui-text-input v-model="values[item.endId]" type="time" :autofocus="false" />
      </div>
    </div>

    <div class="relative h-6 mt-4">
      <div class="absolute top-1/2 h-1 rounded bg-fg-muted opacity-40" style="left: 8px; right: 8px; transform: translateY(-50%)" />
      <div class="absolute top-1/2 h-1 rounded bg-success" :style="{ left: `calc(8px + (100% - 16px) * ${startOffset / RANGE_MAX})`, right: `calc(8px + (100% - 16px) * ${1 - endOffset / RANGE_MAX})`, transform: 'translateY(-50%)' }" />
      <input class="dual-range" type="range" min="0" :max="RANGE_MAX" step="15" :value="startOffset" @input="setStart($event.target.value)" />
      <input class="dual-range" type="range" min="0" :max="RANGE_MAX" step="15" :value="endOffset" @input="setEnd($event.target.value)" />
    </div>
    <div class="relative h-5 pointer-events-none">
      <span v-for="tick in rangeTicks" :key="tick.label" class="absolute text-[10px] text-fg-muted whitespace-nowrap" :style="{ left: `calc(8px + (100% - 16px) * ${tick.at})`, transform: 'translateX(-50%)' }">{{ tick.label }}</span>
    </div>
  </div>

  <div v-else-if="item.type === 'link'" class="py-3 flex items-center">
    <span class="material-symbols text-2xl text-fg-muted">{{ item.icon || 'chevron_right' }}</span>
    <p class="pl-4 flex-grow">{{ label }}</p>
    <span class="material-symbols text-fg-muted">chevron_right</span>
  </div>
</template>

<script>
// The night window slider covers 18:00 to 12:00 the next day, in 15 minute steps
const RANGE_START_MINUTES = 18 * 60
const RANGE_MAX = 18 * 60

function timeToOffset(time) {
  const [h, m] = String(time || '00:00')
    .split(':')
    .map(Number)
  const offset = (h * 60 + m - RANGE_START_MINUTES + 1440) % 1440
  return Math.min(RANGE_MAX, offset)
}

function offsetToTime(offset) {
  const total = (Number(offset) + RANGE_START_MINUTES) % 1440
  return `${String(Math.floor(total / 60)).padStart(2, '0')}:${String(total % 60).padStart(2, '0')}`
}

export default {
  props: {
    item: Object,
    values: Object
  },
  data() {
    return { RANGE_MAX }
  },
  computed: {
    stepIndex() {
      return Math.max(0, (this.item.options || []).indexOf(this.values[this.item.id]))
    },
    startOffset() {
      return this.item.startId ? timeToOffset(this.values[this.item.startId]) : 0
    },
    endOffset() {
      return this.item.endId ? timeToOffset(this.values[this.item.endId]) : 0
    },
    rangeTicks() {
      return [18, 21, 0, 3, 6, 9, 12].map((hour, index) => ({ label: String(hour).padStart(2, '0'), at: (index * 3 * 60) / RANGE_MAX }))
    },
    label() {
      return this.item.label || this.$strings[this.item.labelKey] || this.item.labelKey
    }
  },
  methods: {
    setStep(index) {
      this.values[this.item.id] = this.item.options[Number(index)]
    },
    setStart(offset) {
      this.values[this.item.startId] = offsetToTime(Math.min(Number(offset), this.endOffset - 15))
    },
    setEnd(offset) {
      this.values[this.item.endId] = offsetToTime(Math.max(Number(offset), this.startOffset + 15))
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

<style>
/* Two sliders on top of each other make the start / end handles of the time window */
.dual-range {
  position: absolute;
  left: 0;
  top: 0;
  width: 100%;
  height: 100%;
  margin: 0;
  background: transparent;
  pointer-events: none;
  -webkit-appearance: none;
  appearance: none;
}
.dual-range::-webkit-slider-runnable-track {
  height: 100%;
  background: transparent;
}
.dual-range::-webkit-slider-thumb {
  -webkit-appearance: none;
  pointer-events: auto;
  width: 16px;
  height: 16px;
  margin-top: 4px;
  border-radius: 50%;
  background: #0075ff;
  cursor: pointer;
}
.dual-range::-moz-range-thumb {
  pointer-events: auto;
  width: 16px;
  height: 16px;
  border: none;
  border-radius: 50%;
  background: #0075ff;
  cursor: pointer;
}
</style>
