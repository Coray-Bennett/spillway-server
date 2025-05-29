import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import { useAuthStore } from './stores/auth'
import { updateAuthHeader } from './services/apiService'

import './assets/css/main.css'

// Initialize the app
const app = createApp(App)
const pinia = createPinia()

app.use(pinia)
app.use(router)

// Initialize auth token before app mount
const token = localStorage.getItem('token')
if (token) {
  console.log('[Main] Initializing app with token from localStorage')
  updateAuthHeader(token)
}

// Mount the app
app.mount('#app')

// Get auth store and initialize auth after mount
const authStore = useAuthStore()
authStore.initializeAuth()

console.log('[Main] App initialized')