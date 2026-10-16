<template>
  <!-- Theme pop-up (same container as the app's choice dialog) where every option shows a small preview of the app in that theme -->
  <modals-modal v-model="show" :width="320" height="100%">
    <template #outer>
      <div class="absolute top-10 left-4 z-40 pt-1 pb-1.5" style="max-width: 80%">
        <p class="text-white text-xl truncate">{{ $strings.LabelTheme }}</p>
      </div>
    </template>

    <div class="w-full h-full overflow-hidden absolute top-0 left-0 flex items-center justify-center" @click="show = false">
      <div class="w-full overflow-x-hidden overflow-y-auto bg-primary rounded-lg border border-fg/20 p-2" style="max-height: 85%" @click.stop>
        <ul role="listbox">
          <li v-for="theme in themes" :key="theme.value" role="option" class="select-none cursor-pointer rounded-md p-2 flex items-center" :class="selected === theme.value ? 'bg-success bg-opacity-10' : ''" @click="$emit('action', theme.value)">
            <!-- preview: a small home screen drawn with the colors of this theme -->
            <div class="flex-shrink-0 rounded-md overflow-hidden border border-fg/20 flex flex-col bg-bg" :style="[theme.vars, { width: '92px', height: '156px' }]">
              <div class="h-5 bg-primary flex items-center px-1.5 flex-shrink-0">
                <div class="w-2.5 h-2.5 rounded-full bg-fg-muted" />
                <div class="h-1 w-8 rounded bg-fg ml-1.5 opacity-80" />
              </div>
              <div class="flex-grow p-1.5">
                <div class="flex space-x-1">
                  <div class="w-6 h-8 rounded-sm" style="background: #8b2c2c" />
                  <div class="w-6 h-8 rounded-sm" style="background: #2c5f8b" />
                  <div class="w-6 h-8 rounded-sm" style="background: #8b7a2c" />
                </div>
                <div class="h-1 w-14 rounded bg-fg mt-1.5 opacity-80" />
                <div class="h-1 w-9 rounded bg-fg-muted mt-1" />
                <div class="flex items-center mt-3">
                  <div class="w-5 h-2.5 rounded-full bg-success flex items-center justify-end pr-0.5"><div class="w-1.5 h-1.5 rounded-full bg-white" /></div>
                  <div class="h-1 w-10 rounded bg-fg ml-1.5 opacity-80" />
                </div>
                <div class="flex items-center mt-2">
                  <div class="w-5 h-2.5 rounded-full bg-bg-toggle border border-fg-muted flex items-center pl-0.5"><div class="w-1.5 h-1.5 rounded-full bg-fg-muted" /></div>
                  <div class="h-1 w-8 rounded bg-fg ml-1.5 opacity-80" />
                </div>
              </div>
              <div class="h-6 bg-secondary flex items-center px-1.5 flex-shrink-0">
                <div class="w-3 h-3 rounded-sm" style="background: #8b2c2c" />
                <div class="h-1 w-8 rounded bg-fg ml-1.5 opacity-80" />
              </div>
            </div>

            <p class="flex-grow ml-4 text-base text-fg text-opacity-80">{{ theme.text }}</p>
            <span v-if="selected === theme.value" class="material-symbols text-success mr-1">check</span>
          </li>
        </ul>
      </div>
    </div>
  </modals-modal>
</template>

<script>
// Colors taken from assets/tailwind.css (the app's three themes)
const THEMES = {
  black: { '--color-bg': '0 0 0', '--color-primary': '0 0 0', '--color-secondary': '0 0 0', '--color-fg': '230 237 243', '--color-fg-muted': '120 126 132', '--color-border': '55 62 65', '--color-bg-toggle': '0 0 0', '--color-bg-toggle-selected': '35 35 35' },
  dark: { '--color-bg': '56 56 56', '--color-primary': '35 35 35', '--color-secondary': '47 48 48', '--color-fg': '230 237 243', '--color-fg-muted': '142 147 153', '--color-border': '75 85 89', '--color-bg-toggle': '56 56 56', '--color-bg-toggle-selected': '81 82 84' },
  light: { '--color-bg': '255 255 255', '--color-primary': '222 222 222', '--color-secondary': '246 248 250', '--color-fg': '37 37 37', '--color-fg-muted': '101 109 118', '--color-border': '189 191 191', '--color-bg-toggle': '222 222 222', '--color-bg-toggle-selected': '255 255 255' }
}

export default {
  props: {
    value: Boolean,
    /** 'black' | 'dark' | 'light' */
    selected: String
  },
  computed: {
    show: {
      get() {
        return this.value
      },
      set(val) {
        this.$emit('input', val)
      }
    },
    themes() {
      return [
        { value: 'black', text: this.$strings.LabelThemeBlack },
        { value: 'dark', text: this.$strings.LabelThemeDark },
        { value: 'light', text: this.$strings.LabelThemeLight }
      ].map((t) => ({ ...t, vars: THEMES[t.value] }))
    }
  }
}
</script>
