<template>
  <div>
    <p class="uppercase text-xs font-semibold text-fg-muted mb-2 mt-10">{{ $strings.HeaderVolumeBoost }}</p>
    <p class="text-sm text-fg-muted mb-2">{{ $strings.MessageVolumeBoostDescription }}</p>

    <div class="py-3">
      <div class="flex items-center">
        <p class="flex-grow">{{ $strings.LabelVolumeBoost }}</p>
        <p class="text-sm tabular-nums">{{ boostLabel(volumeBoostDb) }}</p>
      </div>
      <input v-model.number="volumeBoostDb" type="range" min="0" :max="maxBoostDb" step="1" class="w-full" @change="save('volumeBoostDb', volumeBoostDb)" />
    </div>

    <div class="py-3">
      <div class="flex items-center">
        <p class="flex-grow">{{ $strings.LabelAndroidAutoVolumeBoost }}</p>
        <p class="text-sm tabular-nums">{{ boostLabel(androidAutoVolumeBoostDb) }}</p>
      </div>
      <input v-model.number="androidAutoVolumeBoostDb" type="range" min="0" :max="maxBoostDb" step="1" class="w-full" @change="save('androidAutoVolumeBoostDb', androidAutoVolumeBoostDb)" />
      <p class="text-xs text-fg-muted mt-1">{{ $strings.MessageAndroidAutoVolumeBoostDescription }}</p>
    </div>
  </div>
</template>

<script>
import { AbsRbsSettings } from '@/plugins/capacitor/AbsRbsSettings'

export default {
  data() {
    return {
      volumeBoostDb: 0,
      androidAutoVolumeBoostDb: 0,
      maxBoostDb: 15
    }
  },
  methods: {
    boostLabel(db) {
      return db > 0 ? `+${db} dB` : this.$strings.LabelOff
    },
    async save(key, value) {
      try {
        await AbsRbsSettings.setSetting({ key, value })
      } catch (error) {
        console.error('[VolumeBoostSettings] Failed to save', key, error)
        this.$toast.error(this.$strings.ToastFailedToUpdate)
      }
    }
  },
  async mounted() {
    try {
      const settings = await AbsRbsSettings.getSettings()
      this.volumeBoostDb = settings.volumeBoostDb
      this.androidAutoVolumeBoostDb = settings.androidAutoVolumeBoostDb
      this.maxBoostDb = settings.maxBoostDb || 15
    } catch (error) {
      console.error('[VolumeBoostSettings] Failed to load', error)
    }
  }
}
</script>
