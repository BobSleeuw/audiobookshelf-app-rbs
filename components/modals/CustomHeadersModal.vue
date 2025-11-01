<template>
  <modals-modal v-model="show" :width="400" height="100%">
    <template #outer>
      <div class="absolute top-11 left-4 z-40">
        <p class="text-white text-2xl truncate">{{ $strings.HeaderCustomHeaders || 'Custom Headers' }}</p>
      </div>
    </template>

    <div class="w-full h-full overflow-hidden absolute top-0 left-0 flex items-center justify-center" @click="show = false">
      <div class="w-full overflow-x-hidden overflow-y-auto bg-primary rounded-lg border border-border p-4" style="max-height: 80%" @click.stop>
        <connection-custom-headers-settings :custom-headers="localHeaders" @update="handleUpdate" />
      </div>
    </div>
  </modals-modal>
</template>

<script>
export default {
  props: {
    value: Boolean,
    customHeaders: {
      type: Object,
      default: () => ({})
    }
  },
  data() {
    return {
      localHeaders: {}
    }
  },
  watch: {
    show(newVal) {
      if (newVal) {
        // Copy headers when modal opens
        this.localHeaders = { ...this.customHeaders }
      }
    },
    customHeaders: {
      immediate: true,
      handler(val) {
        this.localHeaders = { ...val }
      }
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
    }
  },
  methods: {
    handleUpdate(headers) {
      this.$emit('save', headers)
      this.show = false
    }
  }
}
</script>
