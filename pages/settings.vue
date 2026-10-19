<template>
  <div class="relative w-full h-full flex flex-col overflow-hidden">
    <!-- header bar of a category page (title and back arrow) -->
    <settings-header v-if="currentGroup" :title="currentGroup.title" />

    <div class="flex-grow overflow-y-auto px-4" :class="currentGroup ? 'py-3' : 'py-6'">
      <!-- category list -->
      <template v-if="!currentGroup">
        <p class="uppercase text-xs font-semibold text-fg-muted mb-2">{{ $strings.HeaderSettings }}</p>
        <ui-text-input v-model="query" prepend-icon="search" clearable :autofocus="false" :placeholder="$strings.LabelSearchSettings" class="settings-search mb-5" />

        <!-- search results -->
        <template v-if="query.trim()">
          <p v-if="!results.length" class="py-6 text-center text-fg-muted">{{ $strings.MessageNoSettingsFound }}</p>
          <div v-for="result in results" :key="result.item.id" class="flex items-center py-3 cursor-pointer" @click="openResult(result)">
            <span class="material-symbols text-2xl text-fg-muted w-10">{{ result.group.icon }}</span>
            <div class="flex-grow min-w-0">
              <p class="truncate">{{ result.item.label }}</p>
              <p class="text-xs text-fg-muted truncate">{{ result.group.title }}</p>
            </div>
            <span class="material-symbols text-fg-muted">chevron_right</span>
          </div>
        </template>

        <!-- categories -->
        <template v-else>
          <div v-for="group in groups" :key="group.id" class="flex items-center bg-primary rounded-lg px-4 py-3.5 mb-3 cursor-pointer" @click="openSection(group)">
            <span class="material-symbols text-2xl text-fg-muted w-10 flex-shrink-0">{{ group.icon }}</span>
            <div class="flex-grow min-w-0 pr-2">
              <p class="text-base">{{ group.title }}</p>
              <p class="text-xs text-fg-muted mt-0.5">{{ group.description }}</p>
            </div>
            <span class="material-symbols text-xl text-fg-muted">chevron_right</span>
          </div>
        </template>
      </template>

      <!-- one category -->
      <div v-else ref="section">
        <template v-for="(item, index) in visibleItems">
          <div v-if="(item.dividerBefore || item.headingBefore) && index > 0" :key="item.id + '-divider'" class="w-full h-px bg-fg/10 my-2" />
          <p v-if="item.headingBefore" :key="item.id + '-heading'" class="uppercase text-xs font-semibold text-fg-muted pb-1" :class="index > 0 ? 'pt-2' : 'pt-1'">{{ item.headingBefore }}</p>
          <div :key="item.id" :data-item="item.id" class="rounded transition-colors duration-700" :class="highlightId === item.id ? 'bg-success bg-opacity-20' : ''">
            <settings-row :item="item" :values="values" @set="setValue" @commit="commitValue" @pick="openPicker" @action="runAction" />
          </div>
        </template>
      </div>
    </div>

    <div v-show="loading" class="w-full h-full absolute top-0 left-0 flex items-center justify-center z-10">
      <ui-loading-indicator />
    </div>

    <!-- choice pop-ups: the app's own dialog, and a theme picker with previews -->
    <modals-dialog v-model="showMoreMenuDialog" :items="moreMenuItems" :selected="moreMenuSelected" @action="clickMenuAction" />
    <settings-theme-picker v-model="showThemePicker" :selected="theme" @action="chooseTheme" />
  </div>
</template>

<script>
import { AbsRbsSettings } from '@/plugins/capacitor/AbsRbsSettings'
import { AbsBluetooth } from '@/plugins/capacitor/AbsBluetooth'
import jumpLabelMixin from '@/mixins/jumpLabel'

const SLEEP_TIMER_MINUTES = [5, 10, 15, 30, 45, 60, 90]
const BROWSE_LIMITS = [25, 50, 100, 200, 500]
const DEFAULT_BOOST_DB = 6
const DEFAULT_ANDROID_AUTO_BOOST_DB = 9

export default {
  mixins: [jumpLabelMixin],
  data() {
    return {
      loading: false,
      deviceData: null,
      query: '',
      highlightId: null,
      showMoreMenuDialog: false,
      showThemePicker: false,
      pickerItem: null,
      settings: {
        disableAutoRewind: false,
        enableAltView: true,
        allowSeekingOnMediaControls: false,
        jumpForwardTime: 10,
        jumpBackwardsTime: 10,
        enableMp3IndexSeeking: false,
        disableShakeToResetSleepTimer: false,
        shakeSensitivity: 'MEDIUM',
        lockOrientation: 0,
        hapticFeedback: 'LIGHT',
        autoSleepTimer: false,
        autoSleepTimerStartTime: '22:00',
        autoSleepTimerEndTime: '06:00',
        sleepTimerLength: 900000, // 15 minutes
        disableSleepTimerFadeOut: false,
        disableSleepTimerResetFeedback: false,
        enableSleepTimerAlmostDoneChime: false,
        autoSleepTimerAutoRewind: false,
        autoSleepTimerAutoRewindTime: 300000, // 5 minutes
        languageCode: 'en-us',
        downloadUsingCellular: 'ALWAYS',
        streamingUsingCellular: 'ALWAYS',
        androidAutoBrowseLimitForGrouping: 100,
        androidAutoBrowseSeriesSequenceOrder: 'ASC'
      },
      // ABS-RBS settings (stored natively)
      rbs: {
        progressMode: 'CHAPTER',
        androidAutoProgressMode: 'CHAPTER',
        skipSilenceEnabled: false,
        skipSilenceMaxMs: 500,
        androidAutoQueueMode: 'CHAPTERS',
        bluetoothAutoPlayEnabled: true,
        bluetoothStartWhenClosed: true,
        bluetoothDelaySeconds: 5,
        maxBluetoothDelaySeconds: 30,
        ignoringBatteryOptimizations: true,
        volumeBoostEnabled: false,
        volumeBoostDb: 0,
        androidAutoVolumeBoostDb: 0,
        maxBoostDb: 20
      },
      history: { enabled: true, minSeconds: 60, showBar: true, showBadge: true },
      bluetooth: { loading: false, loaded: false, granted: false, devices: [] },
      theme: 'dark',
      lockCurrentOrientation: false
    }
  },
  computed: {
    isiOS() {
      return this.$platform === 'ios'
    },
    // This is flipped because alt view was the default until v0.9.61-beta
    enableBookshelfView: {
      get() {
        return !this.settings.enableAltView
      },
      set(val) {
        this.settings.enableAltView = !val
      }
    },
    jumpForwardSecondsOptions() {
      return this.$store.state.globals.jumpForwardSecondsOptions || []
    },
    jumpBackwardsSecondsOptions() {
      return this.$store.state.globals.jumpBackwardsSecondsOptions || []
    },
    languageOptionItems() {
      return this.$languageCodeOptions || []
    },
    themeOptionItems() {
      return [
        { text: this.$strings.LabelThemeBlack, value: 'black' },
        { text: this.$strings.LabelThemeDark, value: 'dark' },
        { text: this.$strings.LabelThemeLight, value: 'light' }
      ]
    },
    hapticFeedbackItems() {
      return [
        { text: this.$strings.LabelOff, value: 'OFF' },
        { text: this.$strings.LabelLight, value: 'LIGHT' },
        { text: this.$strings.LabelMedium, value: 'MEDIUM' },
        { text: this.$strings.LabelHeavy, value: 'HEAVY' }
      ]
    },
    shakeSensitivityItems() {
      return [
        { text: this.$strings.LabelVeryLow, value: 'VERY_LOW' },
        { text: this.$strings.LabelLow, value: 'LOW' },
        { text: this.$strings.LabelMedium, value: 'MEDIUM' },
        { text: this.$strings.LabelHigh, value: 'HIGH' },
        { text: this.$strings.LabelVeryHigh, value: 'VERY_HIGH' }
      ]
    },
    cellularItems() {
      return [
        { text: this.$strings.LabelAskConfirmation, value: 'ASK' },
        { text: this.$strings.LabelAlways, value: 'ALWAYS' },
        { text: this.$strings.LabelNever, value: 'NEVER' }
      ]
    },
    queueModeItems() {
      return [
        { text: this.$strings.LabelChapters, value: 'CHAPTERS' },
        { text: this.$strings.LabelFiles, value: 'FILES' }
      ]
    },
    skipSilenceItems() {
      return [100, 200, 300, 400, 500, 600, 700, 800, 900].map((value) => ({ text: `${value / 1000} s`, tick: String(value / 1000), value }))
    },
    historyMinItems() {
      return [15, 30, 60, 120, 300].map((value) => ({ text: this.getJumpLabel(value), value }))
    },
    progressModeItems() {
      return [
        { text: this.$strings.LabelChapterTrack, value: 'CHAPTER' },
        { text: this.$strings.LabelTotalTrack, value: 'BOOK' }
      ]
    },
    seriesOrderItems() {
      return [
        { text: this.$strings.LabelAscending, value: 'ASC' },
        { text: this.$strings.LabelDescending, value: 'DESC' }
      ]
    },
    browseLimitItems() {
      const limits = BROWSE_LIMITS.includes(this.settings.androidAutoBrowseLimitForGrouping) || !this.settings.androidAutoBrowseLimitForGrouping ? BROWSE_LIMITS : [...BROWSE_LIMITS, this.settings.androidAutoBrowseLimitForGrouping].sort((a, b) => a - b)
      return limits.map((n) => ({ text: `${n} ${this.$strings.LabelItems}`, value: n }))
    },
    sleepTimerLengthItems() {
      return SLEEP_TIMER_MINUTES.map((m) => ({ text: `${m} min`, tick: String(m), value: m * 60000 }))
    },

    /** Current value of every setting, by id (what the rows show) */
    values() {
      const s = this.settings
      const values = {
        theme: this.theme,
        language: s.languageCode,
        lockOrientation: this.lockCurrentOrientation,
        bookshelfView: this.enableBookshelfView,
        haptics: s.hapticFeedback,

        btAuto: this.rbs.bluetoothAutoPlayEnabled,
        btDelay: this.rbs.bluetoothDelaySeconds,
        // needs the battery optimization exemption from Android; without it the switch is off and greyed out
        btBackground: this.rbs.bluetoothStartWhenClosed && this.rbs.ignoringBatteryOptimizations,
        autoBrowseLimit: s.androidAutoBrowseLimitForGrouping,
        autoSeriesOrder: s.androidAutoBrowseSeriesSequenceOrder,
        downloadCellular: s.downloadUsingCellular,
        streamCellular: s.streamingUsingCellular,

        jumpBack: s.jumpBackwardsTime,
        jumpForward: s.jumpForwardTime,
        disableAutoRewind: s.disableAutoRewind,
        mp3Seeking: s.enableMp3IndexSeeking,
        mediaSeeking: s.allowSeekingOnMediaControls,
        progressMode: this.rbs.progressMode,
        skipSilence: this.rbs.skipSilenceEnabled,
        skipSilenceMax: this.rbs.skipSilenceMaxMs,
        historyEnabled: this.history.enabled,
        historyMin: this.history.minSeconds,
        historyBar: this.history.showBar,
        historyBadge: this.history.showBadge,
        autoProgressMode: this.rbs.androidAutoProgressMode,
        autoQueueMode: this.rbs.androidAutoQueueMode,
        boostEnabled: this.rbs.volumeBoostEnabled,
        volumeBoost: this.rbs.volumeBoostDb,
        autoBoost: this.rbs.androidAutoVolumeBoostDb,

        shakeReset: !s.disableShakeToResetSleepTimer,
        shakeSensitivity: s.shakeSensitivity,
        disableFade: s.disableSleepTimerFadeOut,
        disableVibrate: s.disableSleepTimerResetFeedback,
        chime: s.enableSleepTimerAlmostDoneChime,
        autoTimer: s.autoSleepTimer,
        autoLength: s.sleepTimerLength,
        autoStart: s.autoSleepTimerStartTime,
        autoEnd: s.autoSleepTimerEndTime,
        autoRewind: s.autoSleepTimerAutoRewind,
        autoRewindTime: s.autoSleepTimerAutoRewindTime
      }
      this.bluetooth.devices.forEach((d) => (values[d.id] = d.enabled))
      return values
    },

    /** All categories with their settings. showIf: ids of toggles that must be on; android: not shown on iOS */
    groups() {
      const s = this.$strings
      const categories = [
        {
          id: 'appearance',
          title: s.LabelSettingsAppearance,
          description: s.MessageSettingsAppearanceDescription,
          icon: 'palette',
          items: [
            { id: 'theme', type: 'select', label: s.LabelTheme, options: this.themeOptionItems },
            { id: 'language', type: 'select', label: s.LabelLanguage, options: this.languageOptionItems },
            { id: 'lockOrientation', type: 'toggle', label: s.LabelLockOrientation, android: true },
            { id: 'bookshelfView', type: 'toggle', label: s.LabelUseBookshelfView },
            { id: 'haptics', type: 'select', label: s.LabelHapticFeedback, options: this.hapticFeedbackItems }
          ]
        },
        {
          id: 'connections',
          title: s.LabelSettingsConnections,
          description: s.MessageSettingsConnectionsDescription,
          icon: 'sync_alt',
          items: [
            { id: 'btAuto', type: 'toggle', label: s.LabelBluetoothAutoPlay, headingBefore: s.LabelBluetooth, android: true },
            { id: 'btDelay', type: 'slider', plain: true, label: s.LabelDelayBeforePlaying, min: 0, max: this.rbs.maxBluetoothDelaySeconds, unit: ' s', showIf: ['btAuto'], android: true },
            { id: 'btBackground', type: 'toggle', label: s.LabelStartWhenAppClosed, disabled: !this.rbs.ignoringBatteryOptimizations, buttonText: s.ButtonGrantPermission, showIf: ['btAuto'], android: true },
            { id: 'btDevices', type: 'devices', label: s.LabelAutoPlayDevices, loading: this.bluetooth.loading, granted: this.bluetooth.granted, devices: this.bluetooth.devices, showIf: ['btAuto'], android: true },
            { id: 'autoProgressMode', type: 'select', label: s.LabelProgressBar, headingBefore: s.LabelAndroidAuto, options: this.progressModeItems, android: true },
            { id: 'autoQueueMode', type: 'select', label: s.LabelAndroidAutoQueue, options: this.queueModeItems, android: true },
            { id: 'autoBrowseLimit', type: 'select', label: s.LabelGroupByLetterAbove, options: this.browseLimitItems, android: true },
            { id: 'autoSeriesOrder', type: 'select', label: s.LabelSeriesBookOrder, options: this.seriesOrderItems, android: true },
            { id: 'downloadCellular', type: 'select', label: s.LabelDownloadUsingCellular, headingBefore: s.LabelMobileData, options: this.cellularItems },
            { id: 'streamCellular', type: 'select', label: s.LabelStreamingUsingCellular, options: this.cellularItems }
          ]
        },
        {
          id: 'playback',
          title: s.LabelSettingsPlayback,
          description: s.MessageSettingsPlaybackDescription,
          icon: 'play_circle',
          items: [
            { id: 'jumpBack', type: 'steps', label: s.LabelJumpBackwardsTime, options: this.jumpItems(this.jumpBackwardsSecondsOptions) },
            { id: 'jumpForward', type: 'steps', label: s.LabelJumpForwardsTime, options: this.jumpItems(this.jumpForwardSecondsOptions) },
            { id: 'disableAutoRewind', type: 'toggle', label: s.LabelDisableAutoRewind, dividerBefore: true },
            { id: 'mp3Seeking', type: 'toggle', label: s.LabelEnableMp3IndexSeeking, android: true },
            { id: 'mediaSeeking', type: 'toggle', label: s.LabelAllowSeekingOnMediaControls },
            { id: 'progressMode', type: 'select', label: s.LabelProgressInNotification, options: this.progressModeItems, android: true },
            { id: 'skipSilence', type: 'toggle', label: s.LabelSkipSilence, dividerBefore: true, android: true },
            { id: 'skipSilenceMax', type: 'steps', label: s.LabelSkipSilenceLongestPause, options: this.skipSilenceItems, showIf: ['skipSilence'], android: true },
            { id: 'historyEnabled', type: 'toggle', label: s.LabelPlaybackHistory, dividerBefore: true },
            { id: 'historyMin', type: 'steps', label: s.LabelHistoryIgnoreShorterThan, options: this.historyMinItems, showIf: ['historyEnabled'] },
            { id: 'historyBar', type: 'toggle', label: s.LabelHistoryShowGoBackBar, showIf: ['historyEnabled'] },
            { id: 'historyBadge', type: 'toggle', label: s.LabelHistoryShowCount, showIf: ['historyEnabled'] },
            { id: 'boostEnabled', type: 'toggle', label: s.LabelVolumeBoost, dividerBefore: true, android: true },
            { id: 'volumeBoost', type: 'slider', label: s.LabelBoostLevel, min: 0, max: this.rbs.maxBoostDb, unit: ' dB', showIf: ['boostEnabled'], android: true },
            { id: 'autoBoost', type: 'slider', label: s.LabelBoostLevelAndroidAuto, min: 0, max: this.rbs.maxBoostDb, unit: ' dB', showIf: ['boostEnabled'], android: true }
          ]
        },
        {
          id: 'sleep',
          title: s.LabelSettingsSleepTimer,
          description: s.MessageSettingsSleepTimerDescription,
          icon: 'bedtime',
          items: [
            { id: 'shakeReset', type: 'toggle', label: s.LabelShakeToReset, android: true },
            { id: 'shakeSensitivity', type: 'select', label: s.LabelShakeSensitivity, options: this.shakeSensitivityItems, showIf: ['shakeReset'], android: true },
            { id: 'disableFade', type: 'toggle', label: s.LabelDisableAudioFadeOut, dividerBefore: true },
            { id: 'disableVibrate', type: 'toggle', label: s.LabelDisableVibrateOnReset, android: true },
            { id: 'chime', type: 'toggle', label: s.LabelSleepTimerAlmostDoneChime, android: true },
            { id: 'autoTimer', type: 'toggle', label: s.LabelAutoSleepTimer, dividerBefore: true, android: true },
            { id: 'autoLength', type: 'steps', label: s.LabelSleepTimerLength, options: this.sleepTimerLengthItems, special: { text: s.LabelEndOfChapter, value: 0 }, defaultValue: 900000, showIf: ['autoTimer'], android: true },
            { id: 'autoWindow', type: 'timerange', label: s.LabelActiveBetween, startId: 'autoStart', endId: 'autoEnd', showIf: ['autoTimer'], android: true },
            { id: 'autoRewind', type: 'toggle', label: s.LabelAutoSleepTimerAutoRewind, showIf: ['autoTimer'], android: true },
            { id: 'autoRewindTime', type: 'steps', label: s.LabelAutoRewindTime, options: this.sleepTimerLengthItems, showIf: ['autoTimer', 'autoRewind'], android: true }
          ]
        }
      ]
      // Drop what does not exist on iOS; every setting remembers the heading it sits under (for search)
      return categories
        .map((group) => {
          let tag = ''
          const items = group.items
            .filter((item) => !(this.isiOS && item.android))
            .map((item) => {
              if (item.headingBefore) tag = item.headingBefore
              return { ...item, tag }
            })
          return { ...group, items }
        })
        .filter((group) => group.items.length)
    },
    currentGroup() {
      const id = this.$route.query.section
      return id ? this.groups.find((g) => g.id === id) || null : null
    },
    visibleItems() {
      return this.currentGroup ? this.currentGroup.items.filter((item) => this.isVisible(item)) : []
    },
    results() {
      const q = this.query.trim().toLowerCase()
      const found = []
      this.groups.forEach((group) => {
        group.items.forEach((item) => {
          if (`${item.label} ${item.tag} ${group.title}`.toLowerCase().includes(q)) found.push({ group, item })
        })
      })
      return found
    },
    moreMenuItems() {
      return this.pickerItem ? this.pickerItem.options : []
    },
    moreMenuSelected() {
      return this.pickerItem ? this.values[this.pickerItem.id] : null
    }
  },
  watch: {
    // Paired devices are only read once the Connections page is opened
    currentGroup(group) {
      if (group && group.id === 'connections' && !this.isiOS && !this.bluetooth.loaded) this.initBluetooth()
    }
  },
  methods: {
    jumpItems(seconds) {
      return seconds.map((value) => ({ text: this.getJumpLabel(value), value }))
    },
    isVisible(item) {
      return (item.showIf || []).every((id) => this.values[id])
    },

    // --- navigation (the open category is in the URL, so the back button closes it) ---
    openSection(group, focusId = null) {
      this.query = ''
      this.highlightId = null
      this.$router.push({ query: { section: group.id } })
      this.$nextTick(() => {
        if (!focusId) return
        const el = this.$refs.section && this.$refs.section.querySelector(`[data-item="${focusId}"]`)
        if (el) el.scrollIntoView({ block: 'center' })
        this.highlightId = focusId
        setTimeout(() => (this.highlightId = null), 1800)
      })
    },
    /** Open the category of a search result and jump to the setting (a setting that only shows under another one jumps to that one) */
    openResult({ group, item }) {
      const off = (item.showIf || []).find((id) => !this.values[id])
      this.openSection(group, off || item.id)
    },

    // --- choosing a value from a list ---
    openPicker(item) {
      this.pickerItem = item
      if (item.id === 'theme') {
        this.showThemePicker = true
      } else {
        this.showMoreMenuDialog = true
      }
    },
    clickMenuAction(value) {
      this.showMoreMenuDialog = false
      if (this.pickerItem) this.setValue(this.pickerItem.id, value)
    },
    chooseTheme(theme) {
      this.showThemePicker = false
      this.setValue('theme', theme)
    },

    // --- changing settings ---
    setValue(id, value) {
      const s = this.settings
      if (id.startsWith('bt:')) return this.toggleBluetoothDevice(id, value)
      switch (id) {
        case 'theme':
          this.theme = value
          return this.saveTheme(value)
        case 'language':
          s.languageCode = value
          break
        case 'lockOrientation':
          return this.setLockOrientation(value)
        case 'bookshelfView':
          this.enableBookshelfView = value
          break
        case 'haptics':
          s.hapticFeedback = value
          return this.hapticFeedbackUpdated(value)

        case 'btAuto':
          this.rbs.bluetoothAutoPlayEnabled = value
          return this.saveRbsSetting('bluetoothAutoPlayEnabled', value)
        case 'btDelay':
          this.rbs.bluetoothDelaySeconds = value // saved when the slider is released
          return
        case 'btBackground':
          this.rbs.bluetoothStartWhenClosed = value
          return this.saveRbsSetting('bluetoothStartWhenClosed', value)
        case 'btAllDevices':
          return this.setAllBluetoothDevices(value)
        case 'autoBrowseLimit':
          s.androidAutoBrowseLimitForGrouping = value
          break
        case 'autoSeriesOrder':
          s.androidAutoBrowseSeriesSequenceOrder = value
          break
        case 'downloadCellular':
          s.downloadUsingCellular = value
          break
        case 'streamCellular':
          s.streamingUsingCellular = value
          break

        case 'jumpBack':
          s.jumpBackwardsTime = value
          break
        case 'jumpForward':
          s.jumpForwardTime = value
          break
        case 'disableAutoRewind':
          s.disableAutoRewind = value
          break
        case 'mp3Seeking':
          s.enableMp3IndexSeeking = value
          break
        case 'mediaSeeking':
          s.allowSeekingOnMediaControls = value
          break
        case 'skipSilence':
          this.rbs.skipSilenceEnabled = value
          return this.saveRbsSetting('skipSilenceEnabled', value)
        case 'skipSilenceMax':
          this.rbs.skipSilenceMaxMs = value
          return this.saveRbsSetting('skipSilenceMaxMs', value)
        case 'historyEnabled':
          return this.saveHistorySetting('enabled', value)
        case 'historyMin':
          return this.saveHistorySetting('minSeconds', value)
        case 'historyBar':
          return this.saveHistorySetting('showBar', value)
        case 'historyBadge':
          return this.saveHistorySetting('showBadge', value)
        case 'progressMode':
          this.rbs.progressMode = value
          return this.saveRbsSetting('progressMode', value)
        case 'autoQueueMode':
          this.rbs.androidAutoQueueMode = value
          return this.saveRbsSetting('androidAutoQueueMode', value)
        case 'autoProgressMode':
          this.rbs.androidAutoProgressMode = value
          return this.saveRbsSetting('androidAutoProgressMode', value)
        case 'boostEnabled':
          return this.setVolumeBoostEnabled(value)
        case 'volumeBoost':
          this.rbs.volumeBoostDb = value // saved when the slider is released
          return
        case 'autoBoost':
          this.rbs.androidAutoVolumeBoostDb = value
          return

        case 'shakeReset':
          s.disableShakeToResetSleepTimer = !value
          break
        case 'shakeSensitivity':
          s.shakeSensitivity = value
          break
        case 'disableFade':
          s.disableSleepTimerFadeOut = value
          break
        case 'disableVibrate':
          s.disableSleepTimerResetFeedback = value
          break
        case 'chime':
          s.enableSleepTimerAlmostDoneChime = value
          break
        case 'autoTimer':
          s.autoSleepTimer = value
          break
        case 'autoLength':
          s.sleepTimerLength = value
          break
        case 'autoStart':
          s.autoSleepTimerStartTime = value
          break
        case 'autoEnd':
          s.autoSleepTimerEndTime = value
          break
        case 'autoRewind':
          s.autoSleepTimerAutoRewind = value
          break
        case 'autoRewindTime':
          s.autoSleepTimerAutoRewindTime = value
          break
        default:
          return
      }
      this.saveSettings()
    },
    /** A slider was released: save its value */
    commitValue(id) {
      if (id === 'btDelay') this.saveRbsSetting('bluetoothDelaySeconds', this.rbs.bluetoothDelaySeconds)
      else if (id === 'volumeBoost') this.saveRbsSetting('volumeBoostDb', this.rbs.volumeBoostDb)
      else if (id === 'autoBoost') this.saveRbsSetting('androidAutoVolumeBoostDb', this.rbs.androidAutoVolumeBoostDb)
    },
    runAction(action) {
      if (action === 'btBackground') this.allowBackgroundStart()
      else if (action === 'btGrant') this.requestBluetoothPermission()
      else if (action === 'btRefresh') this.loadBluetoothDevices()
    },

    async setVolumeBoostEnabled(enabled) {
      this.rbs.volumeBoostEnabled = enabled
      // Switching it on with both levels at zero would do nothing, so start with a sensible level
      if (enabled && !this.rbs.volumeBoostDb && !this.rbs.androidAutoVolumeBoostDb) {
        this.rbs.volumeBoostDb = DEFAULT_BOOST_DB
        this.rbs.androidAutoVolumeBoostDb = DEFAULT_ANDROID_AUTO_BOOST_DB
        await this.saveRbsSetting('volumeBoostDb', DEFAULT_BOOST_DB)
        await this.saveRbsSetting('androidAutoVolumeBoostDb', DEFAULT_ANDROID_AUTO_BOOST_DB)
      }
      await this.saveRbsSetting('volumeBoostEnabled', enabled)
    },

    /** Playback history settings live on the phone; the player is told right away */
    async saveHistorySetting(key, value) {
      this.history = { ...this.history, [key]: value }
      await this.$localStore.setPlaybackHistorySettings(this.history)
      this.$eventBus.$emit('playback-history-settings', this.history)
    },
    async saveRbsSetting(key, value) {
      try {
        await AbsRbsSettings.setSetting({ key, value })
      } catch (error) {
        console.error('[settings] Failed to save', key, error)
        this.$toast.error(this.$strings.ToastFailedToUpdate)
      }
    },
    async loadRbsSettings() {
      try {
        const settings = await AbsRbsSettings.getSettings()
        this.rbs = {
          progressMode: settings.progressMode || 'CHAPTER',
          androidAutoProgressMode: settings.androidAutoProgressMode || 'CHAPTER',
          skipSilenceEnabled: !!settings.skipSilenceEnabled,
          skipSilenceMaxMs: settings.skipSilenceMaxMs || 500,
          androidAutoQueueMode: settings.androidAutoQueueMode || 'CHAPTERS',
          bluetoothAutoPlayEnabled: settings.bluetoothAutoPlayEnabled !== false,
          bluetoothStartWhenClosed: settings.bluetoothStartWhenClosed !== false,
          bluetoothDelaySeconds: settings.bluetoothDelaySeconds,
          maxBluetoothDelaySeconds: settings.maxBluetoothDelaySeconds || 30,
          ignoringBatteryOptimizations: !!settings.ignoringBatteryOptimizations,
          volumeBoostEnabled: !!settings.volumeBoostEnabled,
          volumeBoostDb: settings.volumeBoostDb,
          androidAutoVolumeBoostDb: settings.androidAutoVolumeBoostDb,
          maxBoostDb: settings.maxBoostDb || 20
        }
      } catch (error) {
        console.error('[settings] Failed to load RBS settings', error)
      }
    },

    // --- Bluetooth auto play devices ---
    async allowBackgroundStart() {
      try {
        await AbsRbsSettings.requestIgnoreBatteryOptimizations()
      } catch (error) {
        console.error('[settings] Failed to open battery settings', error)
      }
    },
    async initBluetooth() {
      this.bluetooth.loading = true
      try {
        const perm = await AbsBluetooth.hasBluetoothPermission()
        this.bluetooth.granted = !!perm.granted
        if (this.bluetooth.granted) await this.loadBluetoothDevices()
      } finally {
        this.bluetooth.loading = false
        this.bluetooth.loaded = true
      }
    },
    async requestBluetoothPermission() {
      const res = await AbsBluetooth.requestBluetoothPermission()
      this.bluetooth.granted = !!res.granted
      if (this.bluetooth.granted) await this.loadBluetoothDevices()
    },
    async loadBluetoothDevices() {
      this.bluetooth.loading = true
      try {
        const res = await AbsBluetooth.getDevices()
        this.bluetooth.granted = !!res.granted
        this.bluetooth.devices = (res.devices || []).map((d) => ({
          id: `bt:${d.address}`,
          address: d.address,
          name: d.name,
          enabled: !!d.enabled,
          detail: d.lastConnected ? new Date(d.lastConnected).toLocaleString() : this.$strings.LabelNeverConnected
        }))
      } catch (error) {
        console.error('[settings] Failed to load Bluetooth devices', error)
        this.$toast.error(this.$strings.ToastFailedToLoad)
      } finally {
        this.bluetooth.loading = false
      }
    },
    async toggleBluetoothDevice(id, enabled) {
      const device = this.bluetooth.devices.find((d) => d.id === id)
      if (!device) return
      try {
        await AbsBluetooth.setDeviceEnabled({ address: device.address, name: device.name, enabled })
        device.enabled = enabled
      } catch (error) {
        console.error('[settings] Failed to update device', error)
        this.$toast.error(this.$strings.ToastFailedToUpdate)
      }
    },
    async setAllBluetoothDevices(enabled) {
      for (const device of this.bluetooth.devices) {
        if (device.enabled !== enabled) await this.toggleBluetoothDevice(device.id, enabled)
      }
    },

    // --- original settings storage ---
    saveTheme(theme) {
      document.documentElement.dataset.theme = theme
      this.$localStore.setTheme(theme)
    },
    hapticFeedbackUpdated(val) {
      this.$store.commit('globals/setHapticFeedback', val)
      this.saveSettings()
    },
    getCurrentOrientation() {
      const orientation = window.screen?.orientation || {}
      const type = orientation.type || ''

      if (type.includes('landscape')) return 'LANDSCAPE'
      return 'PORTRAIT' // default
    },
    setLockOrientation(lock) {
      this.lockCurrentOrientation = lock
      this.settings.lockOrientation = lock ? this.getCurrentOrientation() : 'NONE'
      this.$setOrientationLock(this.settings.lockOrientation)
      this.saveSettings()
    },
    async saveSettings() {
      await this.$hapticsImpact()
      const updatedDeviceData = await this.$db.updateDeviceSettings({ ...this.settings })
      if (updatedDeviceData) {
        this.$store.commit('setDeviceData', updatedDeviceData)
        this.deviceData = updatedDeviceData
        this.$setLanguageCode(updatedDeviceData.deviceSettings?.languageCode || 'en-us')
        this.setDeviceSettings()
      }
    },
    setDeviceSettings() {
      const deviceSettings = this.deviceData.deviceSettings || {}
      this.settings.disableAutoRewind = !!deviceSettings.disableAutoRewind
      this.settings.enableAltView = !!deviceSettings.enableAltView
      this.settings.allowSeekingOnMediaControls = !!deviceSettings.allowSeekingOnMediaControls
      this.settings.jumpForwardTime = deviceSettings.jumpForwardTime || 10
      this.settings.jumpBackwardsTime = deviceSettings.jumpBackwardsTime || 10
      this.settings.enableMp3IndexSeeking = !!deviceSettings.enableMp3IndexSeeking

      this.settings.lockOrientation = deviceSettings.lockOrientation || 'NONE'
      this.lockCurrentOrientation = this.settings.lockOrientation !== 'NONE'
      this.settings.hapticFeedback = deviceSettings.hapticFeedback || 'LIGHT'

      this.settings.disableShakeToResetSleepTimer = !!deviceSettings.disableShakeToResetSleepTimer
      this.settings.shakeSensitivity = deviceSettings.shakeSensitivity || 'MEDIUM'
      this.settings.autoSleepTimer = !!deviceSettings.autoSleepTimer
      this.settings.autoSleepTimerStartTime = deviceSettings.autoSleepTimerStartTime || '22:00'
      this.settings.autoSleepTimerEndTime = deviceSettings.autoSleepTimerEndTime || '06:00'
      this.settings.sleepTimerLength = !isNaN(deviceSettings.sleepTimerLength) ? deviceSettings.sleepTimerLength : 900000 // 15 minutes
      this.settings.disableSleepTimerFadeOut = !!deviceSettings.disableSleepTimerFadeOut
      this.settings.disableSleepTimerResetFeedback = !!deviceSettings.disableSleepTimerResetFeedback
      this.settings.enableSleepTimerAlmostDoneChime = !!deviceSettings.enableSleepTimerAlmostDoneChime

      this.settings.autoSleepTimerAutoRewind = !!deviceSettings.autoSleepTimerAutoRewind
      this.settings.autoSleepTimerAutoRewindTime = !isNaN(deviceSettings.autoSleepTimerAutoRewindTime) ? deviceSettings.autoSleepTimerAutoRewindTime : 300000 // 5 minutes

      this.settings.languageCode = deviceSettings.languageCode || 'en-us'

      this.settings.downloadUsingCellular = deviceSettings.downloadUsingCellular || 'ALWAYS'
      this.settings.streamingUsingCellular = deviceSettings.streamingUsingCellular || 'ALWAYS'

      this.settings.androidAutoBrowseLimitForGrouping = deviceSettings.androidAutoBrowseLimitForGrouping || 100
      this.settings.androidAutoBrowseSeriesSequenceOrder = deviceSettings.androidAutoBrowseSeriesSequenceOrder || 'ASC'
    },
    async init() {
      this.loading = true
      this.theme = (await this.$localStore.getTheme()) || 'dark'
      this.history = await this.$localStore.getPlaybackHistorySettings()
      this.deviceData = await this.$db.getDeviceData()
      this.$store.commit('setDeviceData', this.deviceData)
      this.setDeviceSettings()
      if (!this.isiOS) await this.loadRbsSettings()
      this.loading = false
      if (this.currentGroup && this.currentGroup.id === 'connections' && !this.isiOS) this.initBluetooth()
    }
  },
  mounted() {
    // Refresh when returning from the system battery settings dialog
    this.onVisible = () => {
      if (document.visibilityState === 'visible' && !this.isiOS) this.loadRbsSettings()
    }
    document.addEventListener('visibilitychange', this.onVisible)
    this.init()
  },
  beforeDestroy() {
    document.removeEventListener('visibilitychange', this.onVisible)
  }
}
</script>

<style>
/* A smaller search box than the default input */
.settings-search input {
  padding-top: 0.3rem;
  padding-bottom: 0.3rem;
  font-size: 0.875rem;
}
</style>
