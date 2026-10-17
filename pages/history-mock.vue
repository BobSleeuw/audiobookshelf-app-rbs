<template>
  <div class="w-full h-screen overflow-y-auto p-4 bg-bg text-fg">
    <h1 class="text-xl font-semibold">Playback history mockup</h1>
    <p class="text-sm text-fg-muted mb-4">The player screen with a history button (top right). It is a clickable mockup: nothing here is saved.</p>

    <div class="flex flex-wrap gap-6 items-start">
      <div class="rounded-xl border border-fg/20 overflow-hidden flex-shrink-0" style="width: 375px; height: 700px">
        <historymock-player />
      </div>

      <div style="max-width: 420px">
        <p class="uppercase text-xs font-semibold text-fg-muted mb-2">How it works</p>
        <ol class="list-decimal pl-5 space-y-2 text-sm">
          <li>While you listen, the app remembers where you were whenever you make a <b>big jump</b>: dragging the bar, skipping a chapter, a long jump. Small jumps (back 10 s, forward 30 s) are ignored.</li>
          <li>Right after a jump, a bar appears at the bottom: <b>Moved to 5:10:00 · Go back to 3:42:10</b>. It disappears after a few seconds. That is the quick "oops" button.</li>
          <li>The <b>history button</b> (clock with an arrow, top right) shows the last 10 places. Tap one to return. Going back is a jump too, so you can undo that as well.</li>
        </ol>

        <p class="uppercase text-xs font-semibold text-fg-muted mt-6 mb-2">Possible settings (Playback &amp; audio)</p>
        <ul class="list-disc pl-5 space-y-1 text-sm">
          <li>Playback history: on / off</li>
          <li>Ignore jumps shorter than: 30 s / 1 min / 5 min</li>
          <li>Show the "go back" bar after a jump: on / off</li>
        </ul>

        <p class="uppercase text-xs font-semibold text-fg-muted mt-6 mb-2">Where it is stored</p>
        <p class="text-sm">On the phone, per book. Your normal progress still syncs to the server as before; the history is only a safety net on this device.</p>

        <p class="uppercase text-xs font-semibold text-fg-muted mt-6 mb-2">Android Auto</p>
        <p class="text-sm text-fg-muted">The car screen has no room for a history list. A single "back to previous position" button next to the other buttons would be the car version.</p>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  layout: 'blank'
}
</script>
