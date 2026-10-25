# ABS-RBS release notes: 0.14.2-rbs.12.1

Based on the official Audiobookshelf app `v0.14.2-beta` (commit `609f9d64`). Android only.
This release merges the whole ABS-RBS line of work into `master`: 30 commits, 76 source files changed (about +4,200 / -660 lines), plus icons and a new README.

## What changed, by area

### Servers and connections
| Change | Where |
| --- | --- |
| Custom headers (Cloudflare Access style) are sent on every request path: API, WebSocket, downloads, covers, the web view. Kept per server, for all of its addresses. | `server/CustomHeaders.kt`, `HeaderInjectingWebViewClient.kt`, `plugins/AbsWebSocket.kt`, `plugins/axios.js`, `plugins/nativeHttp.js`, `plugins/nativeWebSocket.js` |
| Internal URL and external URL per server, with Wi-Fi name matching and reachability checks. Switches by itself when the network changes. The app never edits the external URL. | `plugins/localNetwork.js`, `layouts/default.vue`, `media/MediaManager.kt`, `device/DeviceManager.kt` |
| Connection icon: green Wi-Fi (internal), cloud (external), busy icon while switching, red "no network" only after 2.5 s. | `components/widgets/ConnectionIndicator.vue`, `store/index.js` |
| One full-width server details screen with a single back button; the back gesture returns to the server list. | `components/connection/ServerConnectForm.vue`, `pages/connect.vue` |
| Same-server check, so downloads and items still match after the address changes (no false "linked to a different server" pop-up; the pop-up shows the alias). | `store/user.js` (`isSameServer`), `pages/item/_id/index.vue`, readers, `ItemMoreMenuModal.vue` |
| Downloaded books are linked to the server entry, not to one of its addresses. | `dadb4b4c` |

### Bluetooth
- Bluetooth auto play: master switch, a toggle per device, delay before playing, and "start when the app is closed".
- Auto reconnect.
- Files: `plugins/AbsBluetooth.kt`, `plugins/capacitor/AbsBluetooth.js`, `settings/RbsSettingsStore.kt`.

### Playback and audio
- **Volume boost** 0 to 20 dB (master switch) with a separate Android Auto level. Fix in this release: the boost is attached again shortly after playback starts, so it works without a nudge (`boost-reattach`).
- **Skip silence** with a "Longest pause kept" slider (0.1 to 0.9 s); off by default (`skip-silence`).
- **Playback history**: history button with a count, a "go back" bar after a jump, the last 10 jumps per book stored on the phone, and settings for ignoring short jumps (15 s to 5 m), the bar and the count.
- Jump times of 60 s are shown as `1m`.

### Settings
- Redesigned in four categories with search: Appearance, Connections & data, Playback & audio, Sleep timer (`pages/settings.vue`, `components/settings/*`).

### Android Auto
- Chapters in the Queue list (with a "Queue shows" setting), progress bar per chapter or for the whole book, next and previous chapter buttons.
- One boost button that steps 2 dB; boost icons show the dB level.
- Connection fix: the address is resolved natively, retried for about three minutes, and a "Server unreachable" tab with a retry button shows in the car (`aa-connection`, `794865aa`).
- Cover art served through an app provider because Cloudflare blocks the car's cover download (`aa-cover-art`, `205202df`).

### App identity
- Package `com.rbs.abs`, own name and icon, so it installs next to the official app.
- The build number is shown in the side menu, for example `0.14.2-beta (RBS 12.1)` (`rbs-version-display`).

## Revert points
Risky features are separate commits with tags, so each can be reverted on its own: `aa-connection`, `aa-cover-art`, `skip-silence`, `boost-reattach`, `rbs-version-display`.

## Not verified yet
Android Auto covers, library tabs and the "Server unreachable" tab, the boost fix, and how skip silence sounds at 0.5 s still need a check on a phone and in the car.
