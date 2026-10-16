import Vue from 'vue'

/**
 * Settings mockups: every setting of the current Settings page grouped in four categories.
 * Labels use the app's real strings (labelKey). Values are local to the mockup and are not saved.
 *
 * item.headingBefore: draw a divider line and a small heading above the setting
 * item.dividerBefore: draw a divider line above the setting
 * item.type: toggle | select | slider | steps | link | time
 * item.showIf: id (or list of ids) of toggles that must be on for the item to show
 * item.help: small explanation shown under the label
 */
const LANGUAGES = ['Български', 'Català', 'Čeština', 'Dansk', 'Deutsch', 'English', 'Español', 'Français', 'Hrvatski', 'Italiano', 'Magyar', 'Nederlands', 'Norsk', 'Polski', 'Português', 'Română', 'Русский', 'Slovenčina', 'Suomi', 'Svenska', 'Українська', '中文', '日本語', '한국어']

// Seconds the app offers for jumping (5, 10, 15, 30, 60, 120, 300), labelled like the real Settings page
const JUMP_OPTIONS = ['5s', '10s', '15s', '30s', '60s', '2m', '5m']

// Paired Bluetooth devices (sample data for the mockup)
const BT_DEVICES = [
  { id: 'bt1', name: 'Car stereo', detail: 'Pioneer MVH-S320BT', default: true },
  { id: 'bt2', name: 'Sony WH-1000XM4', detail: 'Headphones', default: true },
  { id: 'bt3', name: 'JBL Flip 5', detail: 'Speaker', default: false },
  { id: 'bt4', name: 'Galaxy Buds2', detail: 'Earbuds', default: false },
  { id: 'bt5', name: 'Fitbit Charge 5', detail: 'Watch', default: false }
]

// Choices of the real app (SleepTimerLengthModal / AutoSleepTimerRewindLengthModal)
const SLEEP_LENGTHS = ['5 min', '10 min', '15 min', '30 min', '45 min', '60 min', '90 min']
const SLEEP_LENGTH_TICKS = ['5', '10', '15', '30', '45', '60', '90']
const REWIND_LENGTHS = ['5 min', '10 min', '15 min', '30 min', '45 min', '60 min', '90 min']
const REWIND_LENGTH_TICKS = ['5', '10', '15', '30', '45', '60', '90']

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
    id: 'connections',
    title: 'Connections & data',
    shortTitle: 'Connect',
    description: 'Bluetooth, cellular and Android Auto',
    icon: 'sync_alt',
    items: [
      { id: 'btAuto', short: 'Bluetooth auto play', headingBefore: 'Bluetooth', type: 'toggle', label: 'Bluetooth auto play', default: true },
      { id: 'btDelay', short: 'Delay before playing', type: 'slider', plain: true, label: 'Delay before playing', min: 0, max: 30, unit: ' s', default: 5, showIf: 'btAuto' },
      { id: 'btBackground', short: 'Start when app is closed', type: 'action', label: 'Start when the app is closed', buttonText: 'Allow', showIf: 'btAuto' },
      { id: 'btDevices', short: 'Bluetooth devices', type: 'devices', label: 'Play automatically when these devices connect', showIf: 'btAuto', devices: BT_DEVICES },
      { id: 'downloadCellular', short: 'Download using cellular', headingBefore: 'Mobile data', type: 'select', labelKey: 'LabelDownloadUsingCellular', options: ['Ask for confirmation', 'Always', 'Never'], default: 'Ask for confirmation' },
      { id: 'streamCellular', short: 'Stream using cellular', type: 'select', labelKey: 'LabelStreamingUsingCellular', options: ['Ask for confirmation', 'Always', 'Never'], default: 'Always' },
      { id: 'autoBrowseLimit', short: 'Group by letter limit', headingBefore: 'Android Auto', type: 'select', label: 'Group by letter above', options: ['25 items', '50 items', '100 items', '200 items', '500 items'], default: '100 items' },
      { id: 'autoSeriesOrder', short: 'Series book order', type: 'select', label: 'Series book order', options: ['Ascending', 'Descending'], default: 'Ascending' }
    ],
    summary: (v) => (v.btAuto ? 'Bluetooth auto play on' : 'Bluetooth auto play off')
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
      { id: 'shakeReset', short: 'Shake to reset', type: 'toggle', label: 'Shake to reset', help: 'Shake your phone while the timer runs, or within 2 minutes after it ends, to restart it.', default: true },
      { id: 'shakeSensitivity', short: 'Shake sensitivity', type: 'select', labelKey: 'LabelShakeSensitivity', options: ['Very low', 'Low', 'Medium', 'High', 'Very high'], default: 'Medium', showIf: 'shakeReset' },
      { id: 'disableFade', short: 'Audio fade out', dividerBefore: true, type: 'toggle', labelKey: 'LabelDisableAudioFadeOut', help: 'The volume normally fades out during the last minute of the timer.', default: false },
      { id: 'disableVibrate', short: 'Vibrate on reset', type: 'toggle', labelKey: 'LabelDisableVibrateOnReset', help: 'Your phone normally vibrates when the timer is reset.', default: false },
      { id: 'chime', short: 'Almost-done chime', type: 'toggle', labelKey: 'LabelSleepTimerAlmostDoneChime', help: 'Plays a chime when 30 seconds remain.', default: true },
      { id: 'autoTimer', short: 'Auto sleep timer', dividerBefore: true, type: 'toggle', labelKey: 'LabelAutoSleepTimer', help: 'Starts a sleep timer by itself when you play between the start and end time.', default: true, },
      { id: 'autoLength', short: 'Sleep timer length', type: 'steps', label: 'Sleep timer length', options: SLEEP_LENGTHS, ticks: SLEEP_LENGTH_TICKS, special: 'End of chapter', timeText: 'Set time', default: '15 min', showIf: 'autoTimer' },
      { id: 'autoWindow', short: 'Start and end time', type: 'timerange', label: 'Active between', startId: 'autoStart', endId: 'autoEnd', showIf: 'autoTimer' },
      { id: 'autoRewind', short: 'Auto timer rewind', type: 'toggle', labelKey: 'LabelAutoSleepTimerAutoRewind', help: 'When the timer has ended and you press play again, your position moves back a little so you can find where you fell asleep.', default: false, showIf: 'autoTimer' },
      { id: 'autoRewindTime', short: 'Auto rewind time', type: 'steps', labelKey: 'LabelAutoRewindTime', options: REWIND_LENGTHS, ticks: REWIND_LENGTH_TICKS, default: '5 min', showIf: ['autoTimer', 'autoRewind'] }
    ],
    summary: (v) => (v.autoTimer ? `Auto ${v.autoStart}–${v.autoEnd} · ${v.autoLength}` : 'Auto sleep timer off')
  }
]

/** Items shown as tiles in the "quick settings" mockup */
export const QUICK_TILES = [
  { id: 'theme', icon: 'contrast' },
  { id: 'bookshelfView', icon: 'shelves' },
  { id: 'boostEnabled', icon: 'volume_up' },
  { id: 'autoTimer', icon: 'bedtime' },
  { id: 'btAuto', icon: 'bluetooth' },
  { id: 'lockOrientation', icon: 'screen_lock_rotation' }
]

/** Fresh, reactive values for a mockup page */
export function createValues() {
  const values = {}
  GROUPS.forEach((g) => g.items.forEach((i) => (values[i.id] = i.default !== undefined ? i.default : null)))
  GROUPS.forEach((g) => g.items.forEach((i) => (i.devices || []).forEach((d) => (values[d.id] = d.default))))
  values.autoStart = '22:00'
  values.autoEnd = '06:00'
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
  return group.items.filter((i) => isVisible(i, values))
}

/** showIf is the id (or a list of ids) of toggles that must all be on */
export function isVisible(item, values) {
  const deps = [].concat(item.showIf || [])
  return deps.every((id) => values[id]) && (!item.showIfNot || !values[item.showIfNot])
}

/** The setting to jump to for a search result: itself, or the first switched-off toggle that hides it */
export function focusTarget(item, values) {
  const deps = [].concat(item.showIf || [])
  const off = deps.find((id) => !values[id])
  return off || item.id
}
