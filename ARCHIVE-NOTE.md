# Archive note: first iteration (VS Code)

This branch is the **first iteration** of the ABS-RBS custom app, written in VS Code
between 30 October and 11 November 2025.

- It adds custom headers (for example Cloudflare Access) to connecting, playback and downloads.
- It is built on the upstream audiobookshelf-app as it was on 28 October 2025
  (commit 072f19e1), which is now more than 360 upstream commits behind.
- It was previously called `reedit-to-main-master`.

It is **read-only** from now on. Do not develop further on this branch.

The current iteration is the branch `abs-rbs-fullagent`, started on 15 October 2026 from the
current upstream master (609f9d64). It re-implements the custom headers and adds Bluetooth auto
play, volume boost, internal/external server URLs, the new settings, Android Auto improvements
and playback history.
