<template>
  <!-- The server screens use the full width of the screen, like the settings pages -->
  <div class="w-full h-full overflow-y-auto">
    <div class="relative flex flex-col items-center min-h-screen">
      <a aria-label="Back" class="absolute top-2 left-2 z-20 cursor-pointer" @click="back">
        <span class="material-symbols text-4xl">arrow_back</span>
      </a>
      <div class="w-full pt-6 pb-2 flex items-center flex-col justify-center short:hidden">
        <img src="/Logo.png" class="h-20 w-20 mb-2" />
        <h1 class="text-2xl">audiobookshelf</h1>
      </div>
      <p class="hidden short:block self-start pl-14 pt-3 pb-1 text-xl">audiobookshelf</p>

      <connection-server-connect-form v-if="deviceData" ref="form" />

      <div class="flex items-center justify-center mt-auto pt-8 pb-4">
        <a href="https://github.com/advplyr/audiobookshelf-app" target="_blank" class="text-sm pr-2">{{ $strings.MessageFollowTheProjectOnGithub }}</a>
        <a href="https://github.com/advplyr/audiobookshelf-app" target="_blank"
          ><svg class="w-8 h-8 text-fg" xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" aria-hidden="true" role="img" width="32" height="32" preserveAspectRatio="xMidYMid meet" viewBox="0 0 24 24">
            <path
              d="M12 2.247a10 10 0 0 0-3.162 19.487c.5.088.687-.212.687-.475c0-.237-.012-1.025-.012-1.862c-2.513.462-3.163-.613-3.363-1.175a3.636 3.636 0 0 0-1.025-1.413c-.35-.187-.85-.65-.013-.662a2.001 2.001 0 0 1 1.538 1.025a2.137 2.137 0 0 0 2.912.825a2.104 2.104 0 0 1 .638-1.338c-2.225-.25-4.55-1.112-4.55-4.937a3.892 3.892 0 0 1 1.025-2.688a3.594 3.594 0 0 1 .1-2.65s.837-.262 2.75 1.025a9.427 9.427 0 0 1 5 0c1.912-1.3 2.75-1.025 2.75-1.025a3.593 3.593 0 0 1 .1 2.65a3.869 3.869 0 0 1 1.025 2.688c0 3.837-2.338 4.687-4.563 4.937a2.368 2.368 0 0 1 .675 1.85c0 1.338-.012 2.413-.012 2.75c0 .263.187.575.687.475A10.005 10.005 0 0 0 12 2.247z"
              fill="currentColor"
            /></svg
        ></a>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  layout: 'blank',
  data() {
    return {
      deviceData: null
    }
  },
  computed: {},
  methods: {
    /** One back button: inside the server details it goes back to the server list, otherwise it leaves the page */
    back() {
      if (this.$refs.form && this.$refs.form.goBack()) return
      this.$router.push('/')
    },
    async init() {
      await this.$store.dispatch('setupNetworkListener')
      this.deviceData = await this.$db.getDeviceData()
      this.$store.commit('setDeviceData', this.deviceData)
    }
  },
  mounted() {
    // Reset data on logouts
    this.$store.commit('libraries/reset')
    this.$store.commit('setIsFirstLoad', true)
    this.init()
  }
}
</script>
