<template>
  <modals-modal v-model="show" :width="'90%'" :max-width="'420px'" height="100%">
    <div class="w-full h-full overflow-hidden absolute top-0 left-0 flex items-center justify-center" @click="show = false">
      <div ref="container" class="w-full rounded-lg bg-primary border border-white border-opacity-20 overflow-y-auto overflow-x-hidden" style="max-height: 80vh" @click.stop>
        <div class="w-full h-full p-4" v-if="showCloudflare">
          <div class="flex items-center mb-4 -ml-2">
            <ui-icon-btn icon="arrow_back" borderless @click="showCloudflare = false" />
            <p class="text-lg font-semibold ml-1">Cloudflare Access Token</p>
          </div>
          <p class="text-sm text-gray-300 mb-4">Cloudflare Access service token. Sent as <span class="font-mono">CF-Access-Client-Id</span> and <span class="font-mono">CF-Access-Client-Secret</span> headers with every request to this server.</p>
          <form @submit.prevent="submitCloudflare">
            <ui-text-input-with-label v-model="cfClientId" label="Client ID" class="mb-2" />
            <ui-text-input-with-label v-model="cfClientSecret" label="Client Secret" type="password" class="mb-4" />

            <ui-btn type="submit" class="w-full">Save</ui-btn>
          </form>
        </div>
        <div class="w-full h-full p-4" v-else-if="showAddHeader">
          <div class="flex items-center mb-4 -ml-2">
            <ui-icon-btn icon="arrow_back" borderless @click="closeHeaderForm" />
            <p class="text-lg font-semibold ml-1">{{ editingKey ? 'Edit Custom Header' : 'Add Custom Header' }}</p>
          </div>
          <form @submit.prevent="submitForm">
            <ui-text-input-with-label v-model="newHeaderKey" label="Name" class="mb-2" />
            <ui-text-input-with-label v-model="newHeaderValue" label="Value" :type="showValue ? 'text' : 'password'" class="mb-1" />
            <button type="button" class="text-xs text-gray-400 mb-4" @click="showValue = !showValue">{{ showValue ? 'Hide value' : 'Show value' }}</button>

            <ui-btn type="submit" class="w-full">{{ editingKey ? 'Save' : 'Submit' }}</ui-btn>
          </form>
        </div>
        <div class="w-full h-full p-4" v-else>
          <p class="text-lg font-semibold mb-4">Custom Headers</p>
          <template v-for="key in Object.keys(headersCopy)">
            <div :key="key" class="w-full rounded-lg bg-white bg-opacity-5 py-2 pl-4 pr-24 relative mb-2">
              <p class="text-base font-semibold text-gray-200 leading-5 truncate">{{ key }}</p>
              <p class="text-sm text-gray-400">••••••••</p>

              <div class="absolute top-0 bottom-0 right-0 h-full pr-4 flex items-center justify-center space-x-4">
                <button type="button" class="text-gray-300" @click="startEdit(key)"><span class="material-symbols text-lg">edit</span></button>
                <button type="button" class="text-error" @click="removeHeader(key)"><span class="material-symbols text-lg">delete</span></button>
              </div>
            </div>
          </template>
          <p v-if="!Object.keys(headersCopy).length" class="py-4 text-center">No Custom Headers</p>

          <div class="w-full flex justify-center pt-4">
            <ui-btn @click="showAddHeader = true" class="w-full">Add Custom Header</ui-btn>
          </div>
          <div class="w-full flex justify-center pt-2">
            <ui-btn @click="openCloudflare" class="w-full">Add Cloudflare Access Token</ui-btn>
          </div>
        </div>
      </div>
    </div>
  </modals-modal>
</template>

<script>
export default {
  props: {
    value: Boolean,
    customHeaders: {
      type: Object,
      default: () => {}
    }
  },
  data() {
    return {
      newHeaderKey: '',
      newHeaderValue: '',
      editingKey: null,
      showValue: false,
      headersCopy: {},
      showAddHeader: false,
      showCloudflare: false,
      cfClientId: '',
      cfClientSecret: ''
    }
  },
  watch: {
    show(val) {
      if (val) this.init()
    }
  },
  computed: {
    show: {
      get() {
        return this.value
      },
      set(val) {
        this.$emit('input', val)
      }
    }
  },
  methods: {
    startEdit(key) {
      this.editingKey = key
      this.newHeaderKey = key
      this.newHeaderValue = this.headersCopy[key]
      this.showValue = false
      this.showAddHeader = true
    },
    closeHeaderForm() {
      this.showAddHeader = false
      this.editingKey = null
      this.newHeaderKey = ''
      this.newHeaderValue = ''
      this.showValue = false
    },
    openCloudflare() {
      this.cfClientId = this.headersCopy['CF-Access-Client-Id'] || ''
      this.cfClientSecret = this.headersCopy['CF-Access-Client-Secret'] || ''
      this.showCloudflare = true
    },
    submitCloudflare() {
      const id = this.cfClientId.trim()
      const secret = this.cfClientSecret.trim()
      if (id) this.headersCopy['CF-Access-Client-Id'] = id
      else this.$delete(this.headersCopy, 'CF-Access-Client-Id')
      if (secret) this.headersCopy['CF-Access-Client-Secret'] = secret
      else this.$delete(this.headersCopy, 'CF-Access-Client-Secret')
      this.showCloudflare = false
      this.$emit('update:customHeaders', { ...this.headersCopy })
    },
    removeHeader(key) {
      this.$delete(this.headersCopy, key)
      this.$emit('update:customHeaders', { ...this.headersCopy })
    },
    submitForm() {
      const name = this.newHeaderKey.trim()
      if (!name) return
      // Renaming a header replaces the old one
      if (this.editingKey && this.editingKey !== name) this.$delete(this.headersCopy, this.editingKey)
      this.$set(this.headersCopy, name, this.newHeaderValue)
      this.closeHeaderForm()
      this.$emit('update:customHeaders', { ...this.headersCopy })
    },
    init() {
      this.newHeaderKey = ''
      this.newHeaderValue = ''
      this.editingKey = null
      this.showValue = false
      this.showAddHeader = false
      this.showCloudflare = false
      this.headersCopy = this.customHeaders ? { ...this.customHeaders } : {}
    }
  },
  mounted() {}
}
</script>
