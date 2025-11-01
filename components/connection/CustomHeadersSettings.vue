<template>
  <div class="w-full">
    <p class="text-sm text-fg-muted mb-4">{{ $strings.MessageCustomHeadersHelp || 'Add custom HTTP headers for Cloudflare Access or other proxy authentication.' }}</p>

    <!-- Existing headers list -->
    <div v-if="headersList.length" class="mb-4">
      <p class="text-xs font-semibold text-fg mb-2">{{ $strings.LabelCurrentHeaders || 'Current Headers' }}</p>
      <div v-for="(header, index) in headersList" :key="index" class="flex items-center py-2 px-2 mb-2 border border-border rounded-md bg-primary">
        <div class="flex-grow min-w-0">
          <p class="text-sm font-semibold text-fg truncate">{{ header.key }}</p>
          <p class="text-xs text-fg-muted truncate">{{ header.value }}</p>
        </div>
        <span class="material-symbols text-error text-xl cursor-pointer ml-2 flex-shrink-0" @click="removeHeader(index)">delete</span>
      </div>
    </div>

    <div v-else class="mb-4 py-4 text-center">
      <p class="text-sm text-fg-muted">{{ $strings.MessageNoCustomHeaders || 'No custom headers configured' }}</p>
    </div>

    <!-- Add new header form -->
    <div class="mb-4 p-3 border border-border rounded-md bg-bg">
      <p class="text-sm font-semibold text-fg mb-3">{{ $strings.LabelAddNewHeader || 'Add New Header' }}</p>
      <div class="space-y-2">
        <ui-text-input v-model="newHeaderKey" :placeholder="$strings.PlaceholderHeaderName || 'Header Name'" class="w-full" @keyup.enter="addHeader" />
        <ui-text-input v-model="newHeaderValue" :placeholder="$strings.PlaceholderHeaderValue || 'Header Value'" class="w-full" @keyup.enter="addHeader" />
        <ui-btn small color="primary" @click="addHeader" :disabled="!newHeaderKey || !newHeaderValue" class="w-full">
          {{ $strings.ButtonAddHeader || 'Add Header' }}
        </ui-btn>
      </div>
    </div>

    <!-- Save button -->
    <div class="flex justify-end pt-2 border-t border-border">
      <ui-btn color="success" @click="saveHeaders" :disabled="!hasChanges" class="w-full">
        {{ $strings.ButtonSave || 'Save Headers' }}
      </ui-btn>
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
      hasChanges: false,
      initialHeaders: {}
    }
  },
  watch: {
    customHeaders: {
      immediate: true,
      deep: true,
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
      this.initialHeaders = JSON.parse(JSON.stringify(headers || {}))
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
      this.checkForChanges()
    },
    removeHeader(index) {
      this.headersList.splice(index, 1)
      this.checkForChanges()
    },
    checkForChanges() {
      const currentHeaders = {}
      this.headersList.forEach((header) => {
        currentHeaders[header.key] = header.value
      })

      this.hasChanges = JSON.stringify(currentHeaders) !== JSON.stringify(this.initialHeaders)
    },
    saveHeaders() {
      const headers = {}
      this.headersList.forEach((header) => {
        headers[header.key] = header.value
      })

      this.$emit('update', headers)
      this.hasChanges = false
      this.initialHeaders = JSON.parse(JSON.stringify(headers))
    }
  }
}
</script>
