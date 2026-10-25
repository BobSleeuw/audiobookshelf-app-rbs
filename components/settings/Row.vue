<template>
  <!-- One setting of the Settings page. The page owns the values; changes go back with @set -->
  <div v-if="item.type === 'toggle'" class="py-3">
    <div class="flex items-center" :class="item.disabled ? 'opacity-50' : 'cursor-pointer'" @click="toggle">
      <div class="flex-grow pr-4">
        <p>{{ item.label }}</p>
        <p v-if="item.help" class="text-xs text-fg-muted mt-0.5">{{ item.help }}</p>
      </div>
      <ui-toggle-switch :value="!!value" class="pointer-events-none flex-shrink-0" />
    </div>
    <!-- a greyed out switch with the button that gives the permission it needs -->
    <ui-btn v-if="item.disabled && item.buttonText" small class="mt-2" @click="$emit('action', item.id)">{{ item.buttonText }}</ui-btn>
  </div>

  <div v-else-if="item.type === 'select'" class="py-3">
    <div class="flex items-center">
      <p class="flex-grow pr-4">{{ item.label }}</p>
      <div class="flex-shrink-0" @click.stop="$emit('pick', item)">
        <ui-text-input :value="optionText(value)" readonly append-icon="expand_more" :autofocus="false" style="width: 200px" />
      </div>
    </div>
    <p v-if="item.help" class="text-xs text-fg-muted mt-1.5">{{ item.help }}</p>
  </div>

  <div v-else-if="item.type === 'slider'" class="py-3">
    <div class="flex items-center">
      <p class="flex-grow">{{ item.label }}</p>
      <p class="text-sm tabular-nums">{{ item.plain ? value + item.unit : value > 0 ? '+' + value + item.unit : $strings.LabelOff }}</p>
    </div>
    <input :value="value" type="range" :min="item.min" :max="item.max" step="1" class="w-full" @input="slide($event.target.value)" @change="$emit('commit', item.id)" />
  </div>

  <!-- slider that snaps to a fixed list of values, with a dot and a label for every value -->
  <div v-else-if="item.type === 'steps'" class="py-3">
    <div class="flex items-center">
      <p class="flex-grow">{{ item.label }}</p>
      <p class="text-sm tabular-nums">{{ stepText }}</p>
    </div>
    <p v-if="item.help" class="text-xs text-fg-muted mt-0.5">{{ item.help }}</p>

    <!-- "End of chapter" is not a length, so it is a separate choice instead of a point on the slider -->
    <div v-if="item.special" class="mt-2 mb-1">
      <ui-toggle-btns :value="isSpecial ? 'special' : 'time'" :items="[{ text: $strings.LabelSetTime, value: 'time' }, { text: item.special.text, value: 'special' }]" @input="setMode" />
    </div>

    <template v-if="!isSpecial">
      <input :value="stepIndex" type="range" min="0" :max="item.options.length - 1" step="1" class="w-full" @input="setStep($event.target.value)" />
      <div class="flex justify-between px-2 -mt-0.5 pointer-events-none">
        <div v-for="(option, index) in item.options" :key="option.value" class="w-0 flex flex-col items-center">
          <span class="w-1.5 h-1.5 rounded-full" :class="index === stepIndex ? 'bg-success' : 'bg-fg-muted'" />
          <span class="text-[10px] mt-1 whitespace-nowrap" :class="index === stepIndex ? 'text-fg' : 'text-fg-muted'">{{ option.tick || option.text }}</span>
        </div>
      </div>
    </template>
  </div>

  <!-- start and end time on one line; tapping one opens the Android style clock picker -->
  <div v-else-if="item.type === 'timerange'" class="py-3">
    <p>{{ item.label }}</p>
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
    <settings-clock-picker v-model="showClock" :time="clockTime" :title="clockTitle" @confirm="setClockTime" />
  </div>

  <!-- text with a button -->
  <div v-else-if="item.type === 'action'" class="py-3 flex items-center">
    <p class="flex-grow pr-4">{{ item.label }}</p>
    <p v-if="item.done" class="text-sm text-success flex-shrink-0">{{ $strings.LabelAllowed }}</p>
    <ui-btn v-else small class="flex-shrink-0" @click="$emit('action', item.id)">{{ item.buttonText }}</ui-btn>
  </div>

  <!-- list of Bluetooth devices with an on / off toggle for each -->
  <div v-else-if="item.type === 'devices'" class="py-3">
    <!-- the list can be long, so it is folded away until opened -->
    <div class="flex items-center cursor-pointer" @click="devicesOpen = !devicesOpen">
      <p class="flex-grow">{{ item.label }}</p>
      <p v-if="item.granted && item.devices.length" class="text-sm text-fg-muted mr-1">{{ devicesOnCount }} / {{ item.devices.length }}</p>
      <span class="material-symbols text-fg-muted">{{ devicesOpen ? 'expand_less' : 'expand_more' }}</span>
    </div>

    <div v-if="!devicesOpen" />

    <div v-else-if="item.loading" class="py-6 flex justify-center">
      <ui-loading-indicator />
    </div>

    <div v-else-if="!item.granted" class="py-3">
      <p class="mb-3 text-sm text-fg-muted">{{ $strings.MessageBluetoothPermissionRequired }}</p>
      <ui-btn small @click="$emit('action', 'btGrant')">{{ $strings.ButtonGrantPermission }}</ui-btn>
    </div>

    <template v-else>
      <p v-if="!item.devices.length" class="py-4 text-center text-fg-muted">{{ $strings.MessageNoBluetoothDevices }}</p>
      <template v-else>
        <!-- switches every device below on or off -->
        <div class="flex items-center py-2 mt-1 cursor-pointer border-b border-fg/10" @click="$emit('set', 'btAllDevices', !allDevicesOn)">
          <span class="material-symbols text-2xl text-fg-muted w-9 flex-shrink-0">devices</span>
          <p class="flex-grow">{{ $strings.LabelAllDevices }}</p>
          <ui-toggle-switch :value="allDevicesOn" class="pointer-events-none flex-shrink-0" />
        </div>

        <div v-for="device in item.devices" :key="device.id" class="flex items-center py-2 cursor-pointer" @click="$emit('set', device.id, !values[device.id])">
          <span class="material-symbols text-2xl text-fg-muted w-9 flex-shrink-0">{{ values[device.id] ? 'bluetooth_connected' : 'bluetooth' }}</span>
          <div class="flex-grow min-w-0 pr-4">
            <p class="truncate">{{ device.name }}</p>
            <p class="text-xs text-fg-muted truncate">{{ device.detail }}</p>
          </div>
          <ui-toggle-switch :value="!!values[device.id]" class="pointer-events-none flex-shrink-0" />
        </div>
      </template>
      <div class="pt-3 flex justify-center">
        <ui-btn small @click="$emit('action', 'btRefresh')">{{ $strings.ButtonRefresh }}</ui-btn>
      </div>
    </template>
  </div>
</template>

<script>
export default {
  props: {
    item: Object,
    values: Object
  },
  data() {
    return { showClock: false, clockKey: null, clockTitle: '', lastTime: null, devicesOpen: false }
  },
  computed: {
    value() {
      return this.values[this.item.id]
    },
    clockTime() {
      return this.clockKey ? this.values[this.clockKey] : '00:00'
    },
    /** The "special" value (End of chapter) is selected instead of a length */
    isSpecial() {
      return !!this.item.special && this.value === this.item.special.value
    },
    /** Position of the value on the slider (the closest step when the saved value is not one of the steps) */
    stepIndex() {
      const options = this.item.options || []
      let best = 0
      options.forEach((o, i) => {
        if (Math.abs(o.value - this.value) < Math.abs(options[best].value - this.value)) best = i
      })
      return best
    },
    stepText() {
      if (this.isSpecial) return this.item.special.text
      return this.optionText(this.item.options[this.stepIndex].value)
    },
    devicesOnCount() {
      return (this.item.devices || []).filter((d) => this.values[d.id]).length
    },
    allDevicesOn() {
      return (this.item.devices || []).length > 0 && this.item.devices.every((d) => this.values[d.id])
    }
  },
  methods: {
    optionText(value) {
      const option = (this.item.options || []).find((o) => o.value === value)
      return option ? option.text : this.item.format ? this.item.format(value) : String(value)
    },
    toggle() {
      if (this.item.disabled) return
      this.$emit('set', this.item.id, !this.value)
    },
    slide(raw) {
      this.$emit('set', this.item.id, Number(raw))
    },
    setMode(mode) {
      if (mode === 'special') {
        // Remember the length so it comes back when going back to "Set time"
        this.lastTime = this.value
        this.$emit('set', this.item.id, this.item.special.value)
      } else {
        this.$emit('set', this.item.id, this.lastTime || this.item.defaultValue || this.item.options[0].value)
      }
    },
    setStep(index) {
      this.$emit('set', this.item.id, this.item.options[Number(index)].value)
    },
    openClock(key, title) {
      this.clockKey = key
      this.clockTitle = title
      this.showClock = true
    },
    setClockTime(time) {
      this.$emit('set', this.clockKey, time)
    }
  }
}
</script>
