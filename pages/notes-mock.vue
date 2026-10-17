<template>
  <div class="w-full h-screen overflow-y-auto p-4 bg-bg text-fg">
    <h1 class="text-xl font-semibold">Notes and characters mockup</h1>
    <p class="text-sm text-fg-muted mb-4">A book page with two new tabs. Clickable, nothing is saved. Your position in the book is {{ posText }} (so later characters are hidden).</p>

    <div class="flex flex-wrap gap-6 items-start">
      <div class="rounded-xl border border-fg/20 overflow-hidden flex-shrink-0" style="width: 375px; height: 700px">
        <notesmock-book :position="13330" />
      </div>

      <div style="max-width: 440px">
        <p class="uppercase text-xs font-semibold text-fg-muted mb-2">What the server has today</p>
        <p class="text-sm">Audiobookshelf has <b>bookmarks</b> (a short title at a time, saved on the server and shown on all your devices). It has <b>no notes and no character lists</b>. So this would be a feature of the app, not something that already exists on the server.</p>

        <p class="uppercase text-xs font-semibold text-fg-muted mt-6 mb-2">Where could notes and characters be stored?</p>
        <div class="space-y-3 text-sm">
          <div class="rounded-lg bg-primary p-3">
            <p class="font-semibold">A. On this phone only</p>
            <p class="text-success">+ Simple and private, works offline</p>
            <p class="text-warning">− Not on your other devices; lost if you reinstall (unless exported)</p>
          </div>
          <div class="rounded-lg bg-primary p-3">
            <p class="font-semibold">B. Inside the server's bookmarks</p>
            <p>The app saves each note or character as a bookmark with a special title (for example "[note] The signal repeats").</p>
            <p class="text-success">+ Synced to the server and every device, no server change</p>
            <p class="text-warning">− They also show up as normal bookmarks in the web player and other apps; text length is limited</p>
          </div>
          <div class="rounded-lg bg-primary p-3">
            <p class="font-semibold">C. A real server feature</p>
            <p class="text-success">+ Cleanest</p>
            <p class="text-warning">− Needs a change to the Audiobookshelf server itself, which we do not control</p>
          </div>
        </div>
        <p class="text-sm mt-3">My suggestion: start with <b>A</b>, and add an export / import so a list is not lost. Option B is possible later.</p>

        <p class="uppercase text-xs font-semibold text-fg-muted mt-6 mb-2">An automatic character list from the internet?</p>
        <p class="text-sm">Possible in theory, but I would not recommend it:</p>
        <ul class="list-disc pl-5 text-sm space-y-1">
          <li>Character lists on the internet are full of <b>spoilers</b>; the app cannot tell what you have already heard.</li>
          <li>Matching the right book (editions, translations, series) is unreliable.</li>
          <li>It needs an outside service (a search site or an AI service) and sends your book titles there, which the app does not do today.</li>
        </ul>
        <p class="text-sm mt-2">The manual version above avoids all of this. The one smart thing it does: every character remembers <b>where you first met them</b>, and the toggle hides characters you have not met yet, so the list never spoils the story.</p>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  layout: 'blank',
  computed: {
    posText() {
      return '3:42:10'
    }
  }
}
</script>
