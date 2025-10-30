<template>
  <div class="w-full">
    <p class="uppercase text-xs font-semibold text-fg-muted mb-2">{{ $strings.HeaderCustomHeaders || 'Custom Headers' }}</p>

    <!-- Info text -->
    <p class="text-sm text-fg-muted mb-4">Add custom HTTP headers for Cloudflare Access or other proxy authentication.</p>

    <!-- Existing headers list -->
    <div v-if="headersList.length" class="mb-4">
      <div v-for="(header, index) in headersList" :key="index" class="flex items-center py-2 border-b border-fg/10">
        <div class="flex-grow">
          <p class="text-sm font-semibold text-fg">{{ header.key }}</p>
          <p class="text-xs text-fg-muted truncate">{{ header.value }}</p>
        </div>
        <span class="material-symbols text-error text-xl cursor-pointer" @click="removeHeader(index)">delete</span>
      </div>
    </div>

    <!-- Add new header form -->
    <div class="mb-4">
      <p class="text-sm font-semibold text-fg mb-2">Add New Header</p>
      <div class="space-y-2">
        <ui-text-input v-model="newHeaderKey" :placeholder="'Header Name (e.g., CF-Access-Client-Id)'" @keyup.enter="addHeader" />
        <ui-text-input v-model="newHeaderValue" :placeholder="'Header Value'" @keyup.enter="addHeader" />
        <ui-btn small color="success" @click="addHeader" :disabled="!newHeaderKey || !newHeaderValue"> Add Header </ui-btn>
      </div>
    </div>

    <!-- Save button -->
    <div class="flex justify-end">
      <ui-btn color="primary" @click="saveHeaders" :disabled="!hasChanges"> Save Headers </ui-btn>
    </div>
  </div>
</template>

<script>
export default {
  props: {
    customHeaders: {
      type: Object,
      default: () => ({})
    }
  },
  data() {
    return {
      headersList: [],
      newHeaderKey: '',
      newHeaderValue: '',
      hasChanges: false
    }
  },
  watch: {
    customHeaders: {
      immediate: true,
      handler(val) {
        this.loadHeaders(val)
      }
    }
  },
  methods: {
    loadHeaders(headers) {
      this.headersList = Object.keys(headers || {}).map((key) => ({
        key,
        value: headers[key]
      }))
      this.hasChanges = false
    },
    addHeader() {
      if (!this.newHeaderKey || !this.newHeaderValue) return

      // Check if header already exists
      const existingIndex = this.headersList.findIndex((h) => h.key === this.newHeaderKey)
      if (existingIndex >= 0) {
        // Update existing
        this.headersList[existingIndex].value = this.newHeaderValue
      } else {
        // Add new
        this.headersList.push({
          key: this.newHeaderKey,
          value: this.newHeaderValue
        })
      }

      this.newHeaderKey = ''
      this.newHeaderValue = ''
      this.hasChanges = true
    },
    removeHeader(index) {
      this.headersList.splice(index, 1)
      this.hasChanges = true
    },
    saveHeaders() {
      const headers = {}
      this.headersList.forEach((header) => {
        headers[header.key] = header.value
      })

      this.$emit('update', headers)
      this.hasChanges = false
    }
  }
}
</script>
