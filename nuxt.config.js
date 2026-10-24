const pkg = require('./package.json')
// ABS-RBS: build number of this custom build, read from the Android versionName (for example 0.14.2-rbs.12.1)
const rbsMatch = require('fs').readFileSync('./android/app/build.gradle', 'utf8').match(/versionName "[^"]*?rbs\.([\d.]+)"/)

export default {
  ssr: false,
  target: 'static',
  telemetry: false,
  env: {
    PROD: '1',
    ANDROID_APP_URL: 'https://play.google.com/store/apps/details?id=com.audiobookshelf.app',
    IOS_APP_URL: ''
  },

  publicRuntimeConfig: {
    version: pkg.version,
    rbsVersion: rbsMatch ? rbsMatch[1] : ''
  },

  head: {
    title: 'Audiobookshelf',
    htmlAttrs: {
      lang: 'en'
    },
    meta: [{ charset: 'utf-8' }, { name: 'viewport', content: 'viewport-fit=cover, width=device-width, initial-scale=1, user-scalable=no, maximum-scale=1' }, { hid: 'description', name: 'description', content: '' }, { name: 'format-detection', content: 'telephone=no' }],
    script: [
      {
        src: '/libs/sortable.js'
      }
    ],
    link: [{ rel: 'icon', type: 'image/x-icon', href: '/favicon.ico' }]
  },

  css: ['@/assets/tailwind.css', '@/assets/app.css'],

  plugins: ['@/plugins/server.js', '@/plugins/db.js', '@/plugins/localStore.js', '@/plugins/init.client.js', '@/plugins/axios.js', '@/plugins/capacitor/index.js', '@/plugins/capacitor/AbsAudioPlayer.js', '@/plugins/nativeHttp.js', '@/plugins/localNetwork.js', '@/plugins/toast.js', '@/plugins/constants.js', '@/plugins/haptics.js', '@/plugins/i18n.js'],

  components: true,

  modules: ['@nuxtjs/axios'],

  axios: {},

  build: {
    postcss: {
      postcssOptions: {
        plugins: {
          tailwindcss: {},
          autoprefixer: {}
        }
      }
    },
    babel: {
      plugins: [['@babel/plugin-proposal-private-property-in-object', { loose: true }]]
    }
  }
}
