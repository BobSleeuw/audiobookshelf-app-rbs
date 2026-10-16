<template>
  <!-- Side by side comparison of the four settings overview mockups (each frame is a live, clickable page) -->
  <div class="w-full h-screen overflow-y-auto p-4">
    <h1 class="text-xl font-semibold">Settings overview mockups</h1>
    <p class="text-sm text-fg-muted mb-4">Four ideas for grouping the settings. The frames are live: click, toggle and scroll in them. Values are not saved.</p>

    <div class="flex flex-wrap gap-5 items-start">
      <div v-for="mock in mocks" :key="mock.id" :style="{ width: frameWidth * scale + 'px' }">
        <p class="font-semibold mb-1">{{ mock.id.toUpperCase() }} · {{ mock.title }}</p>
        <div class="rounded-xl border border-fg/20 overflow-hidden bg-bg" :style="{ width: frameWidth * scale + 'px', height: frameHeight * scale + 'px' }">
          <div :style="{ width: frameWidth + 'px', height: frameHeight + 'px', zoom: scale }">
            <component :is="`settingsmock-mock-${mock.id}`" />
          </div>
        </div>
        <p class="text-xs text-fg-muted mt-2">{{ mock.description }}</p>
        <ul class="text-xs mt-2 space-y-1">
          <li v-for="pro in mock.pros" :key="pro" class="text-success">+ {{ pro }}</li>
          <li v-for="con in mock.cons" :key="con" class="text-warning">− {{ con }}</li>
        </ul>
        <nuxt-link :to="`/settings-mock/${mock.id}`" class="inline-block text-sm underline mt-2">Open full size</nuxt-link>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  layout: 'blank',
  data() {
    return {
      frameWidth: 375,
      frameHeight: 720,
      scale: 0.62,
      mocks: [
        {
          id: 'a',
          title: 'Category list (drill-down)',
          description: 'A short list of categories with a one-line summary of the current values. Tap one to open a page with only its settings.',
          pros: ['Cleanest overview', 'Familiar (Android / iOS style)', 'Room to grow per category'],
          cons: ['Two taps to reach any setting', 'Cannot see two categories at once']
        },
        {
          id: 'b',
          title: 'Accordion sections',
          description: 'One page, but every category folds open and closed. Only the sections you open take up space.',
          pros: ['Everything stays on one page', 'Quick to open several categories', 'Expand / collapse all'],
          cons: ['Long page when many sections are open', 'Easy to lose your place']
        },
        {
          id: 'c',
          title: 'Tabs',
          description: 'Tabs under the title switch between categories. One category is visible at a time, always one tap away.',
          pros: ['One tap to any category', 'Always shows where you are', 'No back navigation'],
          cons: ['Tabs get cramped with more categories', 'No overview of the values']
        },
        {
          id: 'd',
          title: 'Search + quick tiles',
          description: 'Search on top, tiles for the settings you change most, then category cards showing the main settings with "Show all".',
          pros: ['Fastest for frequent changes', 'Search finds any setting', 'Overview and details together'],
          cons: ['Busiest layout', 'Tiles need choosing / maintaining']
        }
      ]
    }
  }
}
</script>
