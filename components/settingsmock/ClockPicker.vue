<template>
  <!-- Android style (Material) 24 hour time picker: hours and minutes on a round clock face -->
  <modals-modal v-model="show" :width="312" height="100%">
    <div class="w-full h-full overflow-hidden absolute top-0 left-0 flex items-center justify-center" @click="show = false">
      <div class="w-full bg-primary rounded-2xl border border-fg/20 px-5 pt-5 pb-4 select-none" @click.stop>
        <p class="text-sm text-fg-muted mb-3">{{ title }}</p>

        <!-- big hour : minute display -->
        <div class="flex items-center justify-center mb-4">
          <button type="button" class="w-24 h-20 rounded-xl text-6xl tabular-nums flex items-center justify-center" :class="mode === 'hour' ? 'bg-success bg-opacity-25 text-success' : 'bg-fg/10'" @click="mode = 'hour'">{{ pad(hour) }}</button>
          <span class="text-6xl mx-1 pb-2">:</span>
          <button type="button" class="w-24 h-20 rounded-xl text-6xl tabular-nums flex items-center justify-center" :class="mode === 'minute' ? 'bg-success bg-opacity-25 text-success' : 'bg-fg/10'" @click="mode = 'minute'">{{ pad(minute) }}</button>
        </div>

        <!-- clock face -->
        <div ref="dial" class="relative mx-auto rounded-full bg-fg/10 touch-none" :style="{ width: SIZE + 'px', height: SIZE + 'px', touchAction: 'none' }" @pointerdown="onDown" @pointermove="onMove" @pointerup="onUp" @pointercancel="dragging = false">
          <!-- hand and selected circle -->
          <div class="absolute rounded-full bg-success" :style="handStyle" />
          <div class="absolute rounded-full bg-success flex items-center justify-center" :style="selectorStyle">
            <div v-if="mode === 'minute' && minute % 5 !== 0" class="w-1.5 h-1.5 rounded-full bg-white" />
          </div>
          <div class="absolute w-2.5 h-2.5 rounded-full bg-success" :style="{ left: CENTER - 5 + 'px', top: CENTER - 5 + 'px' }" />

          <!-- numbers -->
          <div v-for="n in numbers" :key="n.key" class="absolute flex items-center justify-center rounded-full pointer-events-none" :class="n.selected ? 'text-white' : n.inner ? 'text-fg-muted text-sm' : 'text-fg'" :style="n.style">{{ n.label }}</div>
        </div>

        <div class="flex items-center justify-end space-x-6 mt-4 text-success">
          <button type="button" class="py-2 px-1" @click="show = false">{{ $strings.ButtonCancel }}</button>
          <button type="button" class="py-2 px-1 font-semibold" @click="confirm">{{ $strings.ButtonOk }}</button>
        </div>
      </div>
    </div>
  </modals-modal>
</template>

<script>
const SIZE = 256
const CENTER = SIZE / 2
const OUTER_RADIUS = 100
const INNER_RADIUS = 66
const NUMBER_SIZE = 36

export default {
  props: {
    value: Boolean,
    /** 'HH:MM' (24 hour) */
    time: String,
    title: String
  },
  data() {
    return { SIZE, CENTER, hour: 0, minute: 0, mode: 'hour', dragging: false }
  },
  watch: {
    value(open) {
      if (!open) return
      const [h, m] = String(this.time || '00:00')
        .split(':')
        .map(Number)
      this.hour = h || 0
      this.minute = m || 0
      this.mode = 'hour'
    }
  },
  computed: {
    show: {
      get() {
        return this.value
      },
      set(val) {
        this.$emit('input', val)
      }
    },
    /** Angle (degrees, clockwise from the top) and radius of the selected value */
    selection() {
      if (this.mode === 'hour') {
        const inner = this.hour === 0 || this.hour > 12
        return { angle: (this.hour % 12) * 30, radius: inner ? INNER_RADIUS : OUTER_RADIUS }
      }
      return { angle: this.minute * 6, radius: OUTER_RADIUS }
    },
    selectorStyle() {
      const { x, y } = this.point(this.selection.angle, this.selection.radius)
      return { width: NUMBER_SIZE + 'px', height: NUMBER_SIZE + 'px', left: x - NUMBER_SIZE / 2 + 'px', top: y - NUMBER_SIZE / 2 + 'px' }
    },
    handStyle() {
      const { angle, radius } = this.selection
      return { width: '2px', height: radius + 'px', left: CENTER - 1 + 'px', top: CENTER - radius + 'px', transformOrigin: `1px ${radius}px`, transform: `rotate(${angle}deg)` }
    },
    numbers() {
      const list = []
      if (this.mode === 'hour') {
        for (let i = 1; i <= 12; i++) list.push(this.numberEntry(`o${i}`, String(i), i * 30, OUTER_RADIUS, this.hour === i, false))
        for (let i = 0; i < 12; i++) {
          const hour = i === 0 ? 0 : i + 12
          list.push(this.numberEntry(`i${hour}`, this.pad(hour), i * 30, INNER_RADIUS, this.hour === hour, true))
        }
      } else {
        for (let i = 0; i < 12; i++) list.push(this.numberEntry(`m${i}`, this.pad(i * 5), i * 30, OUTER_RADIUS, this.minute === i * 5, false))
      }
      return list
    }
  },
  methods: {
    pad(n) {
      return String(n).padStart(2, '0')
    },
    point(angle, radius) {
      const rad = (angle * Math.PI) / 180
      return { x: CENTER + radius * Math.sin(rad), y: CENTER - radius * Math.cos(rad) }
    },
    numberEntry(key, label, angle, radius, selected, inner) {
      const { x, y } = this.point(angle, radius)
      return { key, label, selected, inner, style: { width: NUMBER_SIZE + 'px', height: NUMBER_SIZE + 'px', left: x - NUMBER_SIZE / 2 + 'px', top: y - NUMBER_SIZE / 2 + 'px' } }
    },
    /** Turns a pointer position on the dial into an hour or a minute */
    pick(event) {
      const rect = this.$refs.dial.getBoundingClientRect()
      const scale = rect.width / SIZE
      const dx = (event.clientX - rect.left) / scale - CENTER
      const dy = (event.clientY - rect.top) / scale - CENTER
      const angle = (Math.atan2(dx, -dy) * 180) / Math.PI
      const degrees = (angle + 360) % 360
      if (this.mode === 'hour') {
        const index = Math.round(degrees / 30) % 12
        const outer = Math.hypot(dx, dy) > (OUTER_RADIUS + INNER_RADIUS) / 2
        this.hour = outer ? (index === 0 ? 12 : index) : index === 0 ? 0 : index + 12
      } else {
        this.minute = Math.round(degrees / 6) % 60
      }
    },
    onDown(event) {
      this.dragging = true
      this.$refs.dial.setPointerCapture && this.$refs.dial.setPointerCapture(event.pointerId)
      this.pick(event)
    },
    onMove(event) {
      if (this.dragging) this.pick(event)
    },
    onUp() {
      if (!this.dragging) return
      this.dragging = false
      // After choosing the hour, go on to the minutes (like the Android picker)
      if (this.mode === 'hour') this.mode = 'minute'
    },
    confirm() {
      this.$emit('confirm', `${this.pad(this.hour)}:${this.pad(this.minute)}`)
      this.show = false
    }
  }
}
</script>
