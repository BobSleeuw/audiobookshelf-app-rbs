import Vue from 'vue'

/**
 * Settings mockups: every setting of the current Settings page grouped in four categories.
 * Labels use the app's real strings (labelKey). Values are local to the mockup and are not saved.
 *
 * item.type: toggle | select | slider | link | time
 * item.showIf: id of a toggle that must be on for the item to show
 */
export const GROUPS = [
  {
    id: 'appearance',
    title: 'Appearance',
    icon: 'palette',
    items: [
      { id: 'bookshelfView', type: 'toggle', labelKey: 'LabelUseBookshelfView', default: true },
      { id: 'theme', type: 'select', labelKey: 'LabelTheme', options: ['Dark', 'Light', 'Black'], default: 'Dark' },
      { id: 'language', type: 'select', labelKey: 'LabelLanguage', options: ['English', 'Nederlands', 'Deutsch'], default: 'English' },
      { id: 'lockOrientation', type: 'toggle', labelKey: 'LabelLockOrientation', default: false },
      { id: 'haptics', type: 'select', labelKey: 'LabelHapticFeedback', options: ['None', 'Light', 'Medium', 'Heavy'], default: 'Light' }
    ],
    summary: (v) => `${v.theme} theme · ${v.language} · ${v.bookshelfView ? 'Bookshelf view' : 'List view'}`
  },
  {
    id: 'playback',
    title: 'Playback & audio',
    shortTitle: 'Playback',
    icon: 'play_circle',
    items: [
      { id: 'jumpBack', type: 'select', labelKey: 'LabelJumpBackwardsTime', options: ['5s', '10s', '15s', '30s'], default: '10s' },
      { id: 'jumpForward', type: 'select', labelKey: 'LabelJumpForwardsTime', options: ['10s', '30s', '60s'], default: '30s' },
      { id: 'disableAutoRewind', type: 'toggle', labelKey: 'LabelDisableAutoRewind', default: false },
      { id: 'mp3Seeking', type: 'toggle', labelKey: 'LabelEnableMp3IndexSeeking', default: false },
      { id: 'mediaSeeking', type: 'toggle', labelKey: 'LabelAllowSeekingOnMediaControls', default: false },
      { id: 'volumeBoost', type: 'slider', labelKey: 'LabelVolumeBoost', min: 0, max: 15, unit: ' dB', default: 6 },
      { id: 'autoBoost', type: 'slider', labelKey: 'LabelAndroidAutoVolumeBoost', min: 0, max: 15, unit: ' dB', default: 9 }
    ],
    summary: (v) => `Jump ${v.jumpBack} / ${v.jumpForward} · Boost ${v.volumeBoost ? '+' + v.volumeBoost + ' dB' : 'off'}`
  },
  {
    id: 'sleep',
    title: 'Sleep timer',
    icon: 'bedtime',
    items: [
      { id: 'disableShake', type: 'toggle', labelKey: 'LabelDisableShakeToReset', default: false },
      { id: 'shakeSensitivity', type: 'select', labelKey: 'LabelShakeSensitivity', options: ['Very low', 'Low', 'Medium', 'High'], default: 'Medium', showIfNot: 'disableShake' },
      { id: 'disableFade', type: 'toggle', labelKey: 'LabelDisableAudioFadeOut', default: false },
      { id: 'disableVibrate', type: 'toggle', labelKey: 'LabelDisableVibrateOnReset', default: false },
      { id: 'chime', type: 'toggle', labelKey: 'LabelSleepTimerAlmostDoneChime', default: true },
      { id: 'autoTimer', type: 'toggle', labelKey: 'LabelAutoSleepTimer', default: true },
      { id: 'autoStart', type: 'time', labelKey: 'LabelStartTime', default: '22:00', showIf: 'autoTimer' },
      { id: 'autoEnd', type: 'time', labelKey: 'LabelEndTime', default: '06:00', showIf: 'autoTimer' },
      { id: 'autoLength', type: 'select', labelKey: 'LabelSleepTimer', options: ['15 min', '30 min', '45 min', '60 min'], default: '15 min', showIf: 'autoTimer' },
      { id: 'autoRewind', type: 'toggle', labelKey: 'LabelAutoSleepTimerAutoRewind', default: false, showIf: 'autoTimer' }
    ],
    summary: (v) => (v.autoTimer ? `Auto ${v.autoStart}–${v.autoEnd} · ${v.autoLength}` : 'Auto sleep timer off')
  },
  {
    id: 'connections',
    title: 'Connections & data',
    shortTitle: 'Connect',
    icon: 'sync_alt',
    items: [
      { id: 'bluetooth', type: 'link', labelKey: 'HeaderBluetoothAutoPlay', valueText: '2 devices on · 5 s delay' },
      { id: 'autoBrowseLimit', type: 'select', labelKey: 'LabelAndroidAutoBrowseLimitForGrouping', options: ['50', '100', '200'], default: '100' },
      { id: 'autoSeriesOrder', type: 'select', labelKey: 'LabelAndroidAutoBrowseSeriesSequenceOrder', options: ['Ascending', 'Descending'], default: 'Ascending' },
      { id: 'downloadCellular', type: 'select', labelKey: 'LabelDownloadUsingCellular', options: ['Ask', 'Always', 'Never'], default: 'Ask' },
      { id: 'streamCellular', type: 'select', labelKey: 'LabelStreamingUsingCellular', options: ['Ask', 'Always', 'Never'], default: 'Always' }
    ],
    summary: (v) => `Bluetooth · Android Auto · Cellular: ${v.downloadCellular.toLowerCase()} downloads`
  }
]

/** Items shown as tiles in the "quick settings" mockup */
export const QUICK_TILES = [
  { id: 'theme', icon: 'contrast' },
  { id: 'bookshelfView', icon: 'shelves' },
  { id: 'volumeBoost', icon: 'volume_up' },
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
