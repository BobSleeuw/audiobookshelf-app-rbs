import { CapacitorHttp } from '@capacitor/core'

export default function ({ $axios, store, $db }) {
  /**
   * Custom headers (e.g. Cloudflare Access) must be sent on requests to the server. A browser XHR would trigger a
   * CORS preflight that gets blocked by such proxies, so requests to the server go through native HTTP instead.
   */
  const nativeAdapter = async (config) => {
    const url = $axios.getUri(config)
    const headers = {}
    Object.entries({ ...config.headers.common, ...config.headers[config.method], ...config.headers }).forEach(([k, v]) => {
      if (typeof v === 'string' || typeof v === 'number') headers[k] = String(v)
    })
    const res = await CapacitorHttp.request({
      method: (config.method || 'get').toUpperCase(),
      url,
      headers,
      data: config.data && typeof config.data === 'string' ? JSON.parse(config.data) : config.data,
      responseType: config.responseType === 'arraybuffer' ? 'arraybuffer' : config.responseType === 'blob' ? 'blob' : undefined,
      connectTimeout: config.timeout || undefined
    })
    let data = res.data
    if (config.responseType === 'arraybuffer' && typeof data === 'string') {
      const bin = atob(data)
      const bytes = new Uint8Array(bin.length)
      for (let i = 0; i < bin.length; i++) bytes[i] = bin.charCodeAt(i)
      data = bytes.buffer
    }
    const response = { data, status: res.status, statusText: String(res.status), headers: res.headers || {}, config, request: {} }
    if (res.status >= 200 && res.status < 300) return response
    const error = new Error(`Request failed with status code ${res.status}`)
    error.config = config
    error.response = response
    error.isAxiosError = true
    throw error
  }

  // Track if we're currently refreshing to prevent multiple refresh attempts
  let isRefreshing = false
  let failedQueue = []

  const processQueue = (error, token = null) => {
    failedQueue.forEach(({ resolve, reject }) => {
      if (error) {
        reject(error)
      } else {
        resolve(token)
      }
    })
    failedQueue = []
  }

  /**
   * Handles the case when token refresh fails
   * @param {string} [serverConnectionConfigId]
   * @returns {Promise} - Promise that resolves when logout is complete
   */
  const handleRefreshFailure = async (serverConnectionConfigId) => {
    try {
      console.log('[axios] Handling refresh failure - logging out user')

      // Clear store
      await store.dispatch('user/logout')

      if (serverConnectionConfigId) {
        // Clear refresh token for server connection config
        await $db.clearRefreshToken(serverConnectionConfigId)
      }

      if (window.location.pathname !== '/connect') {
        window.location.href = '/connect?error=refreshTokenFailed&serverConnectionConfigId=' + serverConnectionConfigId
      }
    } catch (error) {
      console.error('[axios] Failed to handle refresh failure:', error)
    }
  }

  $axios.onRequest((config) => {
    console.log('[Axios] Making request to ' + config.url)
    const customHeaders = store.state.user.serverConnectionConfig?.customHeaders
    const hasCustomHeaders = customHeaders && Object.keys(customHeaders).length > 0
    const applyCustomHeaders = () => {
      Object.entries(customHeaders).forEach(([k, v]) => {
        config.headers.common[k] = v
      })
      config.adapter = nativeAdapter
    }

    if (config.url.startsWith('http:') || config.url.startsWith('https:') || config.url.startsWith('capacitor:')) {
      // Absolute URLs only get the custom headers when they target the connected server
      const serverAddress = store.getters['user/getServerAddress']
      if (hasCustomHeaders && serverAddress && config.url.startsWith(serverAddress)) applyCustomHeaders()
      return
    }

    if (hasCustomHeaders) applyCustomHeaders()

    const bearerToken = store.getters['user/getToken']
    if (bearerToken) {
      config.headers.common['Authorization'] = `Bearer ${bearerToken}`
    } else {
      console.warn('[Axios] No Bearer Token for request')
    }

    const serverUrl = store.getters['user/getServerAddress']
    if (serverUrl) {
      config.url = `${serverUrl}${config.url}`
    }
    console.log('[Axios] Request out', config.url)
  })

  $axios.onError(async (error) => {
    const originalRequest = error.config
    const code = parseInt(error.response && error.response.status)
    const message = error.response ? error.response.data || 'Unknown Error' : 'Unknown Error'

    console.error('Axios error', code, message)

    // Handle 401 Unauthorized (token expired)
    if (code === 401 && !originalRequest._retry) {
      // Skip refresh for auth endpoints to prevent infinite loops
      if (originalRequest.url.endsWith('/auth/refresh') || originalRequest.url.endsWith('/login')) {
        await handleRefreshFailure(store.getters['user/getServerConnectionConfigId'])
        return Promise.reject(error)
      }

      if (isRefreshing) {
        // If already refreshing, queue this request
        return new Promise((resolve, reject) => {
          failedQueue.push({ resolve, reject })
        })
          .then((token) => {
            if (!originalRequest.headers) {
              originalRequest.headers = {}
            }
            originalRequest.headers['Authorization'] = `Bearer ${token}`
            return $axios(originalRequest)
          })
          .catch((err) => {
            return Promise.reject(err)
          })
      }

      originalRequest._retry = true
      isRefreshing = true

      try {
        // Attempt to refresh the token
        // Updates store if successful, otherwise clears store and throw error
        const newAccessToken = await store.dispatch('user/refreshToken')
        if (!newAccessToken) {
          console.error('No new access token received')
          return Promise.reject(error)
        }

        // Update the original request with new token
        if (!originalRequest.headers) {
          originalRequest.headers = {}
        }
        originalRequest.headers['Authorization'] = `Bearer ${newAccessToken}`

        // Process any queued requests
        processQueue(null, newAccessToken)

        // Retry the original request
        return $axios(originalRequest)
      } catch (refreshError) {
        console.error('Token refresh failed:', refreshError)

        // Process queued requests with error
        processQueue(refreshError, null)

        // Transient errors (offline, proxy/tunnel hiccup) keep the session so the next request can retry
        if (refreshError?.transient) return Promise.reject(error)

        await handleRefreshFailure(store.getters['user/getServerConnectionConfigId'])

        return Promise.reject(refreshError)
      } finally {
        isRefreshing = false
      }
    }

    return Promise.reject(error)
  })
}
