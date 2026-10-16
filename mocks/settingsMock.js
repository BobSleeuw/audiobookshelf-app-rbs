import Vue from 'vue'

/**
 * Settings mockups: every setting of the current Settings page grouped in four categories.
 * Labels use the app's real strings (labelKey). Values are local to the mockup and are not saved.
 *
 * item.dividerBefore: draw a divider line above the setting
 * item.type: toggle | select | slider | steps | link | time
 * item.showIf: id of a toggle that must be on for the item to show
 */
const LANGUAGES = ['Български', 'Català', 'Čeština', 'Dansk', 'Deutsch', 'English', 'Español', 'Français', 'Hrvatski', 'Italiano', 'Magyar', 'Nederlands', 'Norsk', 'Polski', 'Português', 'Română', 'Русский', 'Slovenčina', 'Suomi', 'Svenska', 'Українська', '中文', '日本語', '한국어']

// Seconds the app offers for jumping (5, 10, 15, 30, 60, 120, 300), labelled like the real Settings page
const JUMP_OPTIONS = ['5s', '10s', '15s', '30s', '60s', '2m', '5m']

export const GROUPS = [
  {
    id: 'appearance',
    title: 'Appearance',
    description: 'Theme, language and display',
    icon: 'palette',
    items: [
      { id: 'theme', short: 'Theme', type: 'select', labelKey: 'LabelTheme', options: ['Black', 'Dark', 'Light'], default: 'Dark' },
      { id: 'language', short: 'Language', type: 'select', labelKey: 'LabelLanguage', options: LANGUAGES, default: 'English' },
      { id: 'lockOrientation', short: 'Lock orientation', type: 'toggle', labelKey: 'LabelLockOrientation', default: false },
      { id: 'bookshelfView', short: 'Bookshelf view', type: 'toggle', labelKey: 'LabelUseBookshelfView', default: true },
      { id: 'haptics', short: 'Haptic feedback', type: 'select', labelKey: 'LabelHapticFeedback', options: ['None', 'Light', 'Medium', 'Heavy'], default: 'Light' }
    ],
    summary: (v) => `${v.theme} theme · ${v.language} · ${v.bookshelfView ? 'Bookshelf view' : 'List view'}`
  },
  {
    id: 'playback',
    title: 'Playback & audio',
    shortTitle: 'Playback',
    description: 'Jumping, seeking and volume boost',
    icon: 'play_circle',
    items: [
      { id: 'jumpBack', short: 'Jump back', type: 'steps', labelKey: 'LabelJumpBackwardsTime', options: JUMP_OPTIONS, default: '10s' },
      { id: 'jumpForward', short: 'Jump forward', type: 'steps', labelKey: 'LabelJumpForwardsTime', options: JUMP_OPTIONS, default: '30s' },
      { id: 'disableAutoRewind', short: 'Auto rewind', dividerBefore: true, type: 'toggle', labelKey: 'LabelDisableAutoRewind', default: false },
      { id: 'mp3Seeking', short: '.mp3 seeking', type: 'toggle', label: 'Enable .mp3 index seeking', default: false },
      { id: 'mediaSeeking', short: 'Media control seeking', type: 'toggle', labelKey: 'LabelAllowSeekingOnMediaControls', default: false },
      { id: 'boostEnabled', short: 'Volume boost', dividerBefore: true, type: 'toggle', labelKey: 'LabelVolumeBoost', default: true },
      { id: 'volumeBoost', short: 'Boost level', type: 'slider', label: 'Boost level', min: 0, max: 15, unit: ' dB', default: 6, showIf: 'boostEnabled' },
      { id: 'autoBoost', short: 'Android Auto boost level', type: 'slider', label: 'Boost level on Android Auto', min: 0, max: 15, unit: ' dB', default: 9, showIf: 'boostEnabled' }
    ],
    summary: (v) => `Jump ${v.jumpBack} / ${v.jumpForward} · Boost ${v.boostEnabled ? '+' + v.volumeBoost + ' dB' : 'off'}`
  },
  {
    id: 'sleep',
    title: 'Sleep timer',
    description: 'Auto timer, shaking and fade out',
    icon: 'bedtime',
    items: [
      { id: 'disableShake', short: 'Shake to reset', type: 'toggle', labelKey: 'LabelDisableShakeToReset', default: false },
      { id: 'shakeSensitivity', short: 'Shake sensitivity', type: 'select', labelKey: 'LabelShakeSensitivity', options: ['Very low', 'Low', 'Medium', 'High'], default: 'Medium', showIfNot: 'disableShake' },
      { id: 'disableFade', short: 'Fade out', type: 'toggle', labelKey: 'LabelDisableAudioFadeOut', default: false },
      { id: 'disableVibrate', short: 'Vibrate on reset', type: 'toggle', labelKey: 'LabelDisableVibrateOnReset', default: false },
      { id: 'chime', short: 'Almost-done chime', type: 'toggle', labelKey: 'LabelSleepTimerAlmostDoneChime', default: true },
      { id: 'autoTimer', short: 'Auto sleep timer', type: 'toggle', labelKey: 'LabelAutoSleepTimer', default: true },
      { id: 'autoStart', short: 'Auto timer start time', type: 'time', labelKey: 'LabelStartTime', default: '22:00', showIf: 'autoTimer' },
      { id: 'autoEnd', short: 'Auto timer end time', type: 'time', labelKey: 'LabelEndTime', default: '06:00', showIf: 'autoTimer' },
      { id: 'autoLength', short: 'Auto timer length', type: 'select', labelKey: 'LabelSleepTimer', options: ['15 min', '30 min', '45 min', '60 min'], default: '15 min', showIf: 'autoTimer' },
      { id: 'autoRewind', short: 'Auto timer rewind', type: 'toggle', labelKey: 'LabelAutoSleepTimerAutoRewind', default: false, showIf: 'autoTimer' }
    ],
    summary: (v) => (v.autoTimer ? `Auto ${v.autoStart}–${v.autoEnd} · ${v.autoLength}` : 'Auto sleep timer off')
  },
  {
    id: 'connections',
    title: 'Connections & data',
    shortTitle: 'Connect',
    description: 'Bluetooth, Android Auto, cellular',
    icon: 'sync_alt',
    items: [
      { id: 'bluetooth', short: 'Bluetooth auto play', icon: 'bluetooth', type: 'link', labelKey: 'HeaderBluetoothAutoPlay', valueText: '2 devices on · 5 s delay' },
      { id: 'autoBrowseLimit', short: 'Android Auto browse limit', type: 'select', labelKey: 'LabelAndroidAutoBrowseLimitForGrouping', options: ['50', '100', '200'], default: '100' },
      { id: 'autoSeriesOrder', short: 'Android Auto series order', type: 'select', labelKey: 'LabelAndroidAutoBrowseSeriesSequenceOrder', options: ['Ascending', 'Descending'], default: 'Ascending' },
      { id: 'downloadCellular', short: 'Download on cellular', type: 'select', labelKey: 'LabelDownloadUsingCellular', options: ['Ask', 'Always', 'Never'], default: 'Ask' },
      { id: 'streamCellular', short: 'Stream on cellular', type: 'select', labelKey: 'LabelStreamingUsingCellular', options: ['Ask', 'Always', 'Never'], default: 'Always' }
    ],
    summary: (v) => `Bluetooth · Android Auto · Cellular: ${v.downloadCellular.toLowerCase()} downloads`
  }
]

/** Items shown as tiles in the "quick settings" mockup */
export const QUICK_TILES = [
  { id: 'theme', icon: 'contrast' },
  { id: 'bookshelfView', icon: 'shelves' },
  { id: 'boostEnabled', icon: 'volume_up' },
  { id: 'autoTimer', icon: 'bedtime' },
  { id: 'bluetooth', icon: 'bluetooth' },
  { id: 'lockOrientation', icon: 'screen_lock_rotation' }
]

/** Fresh, reactive values for a mockup page */
export function createValues() {
  const values = {}
  GROUPS.forEach((g) => g.items.forEach((i) => (values[i.id] = i.default !== undefined ? i.default : null)))
  return Vue.observable(values)
}

export function findItem(id) {
  for (const g of GROUPS) {
    const item = g.items.find((i) => i.id === id)
    if (item) return item
  }
  return null
}

/** Items of a group that are visible with the current values (some depend on a toggle) */
export function visibleItems(group, values) {
  return group.items.filter((i) => (!i.showIf || values[i.showIf]) && (!i.showIfNot || !values[i.showIfNot]))
}
