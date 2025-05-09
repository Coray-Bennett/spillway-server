<template>
  <div class="app">
    <header class="app-header">
      <nav class="nav-container">
        <div class="nav-brand">
          <router-link to="/" class="brand-link">
            <svg width="32" height="32" viewBox="0 0 32 32" fill="currentColor">
              <path d="M16 0L3 8v8c0 8.837 5.163 16 13 16s13-7.163 13-16V8L16 0zm-4 16a4 4 0 118 0 4 4 0 01-8 0z"/>
            </svg>
            <span class="brand-text">Spillway</span>
          </router-link>
        </div>
        
        <div class="nav-links" v-if="authStore.isAuthenticated">
          <router-link to="/upload" class="nav-link">Upload</router-link>
          <router-link to="/videos" class="nav-link">My Videos</router-link>
          <router-link to="/playlists" class="nav-link">Playlists</router-link>
          <button @click="handleLogout" class="btn btn-danger nav-btn">
            Logout
          </button>
        </div>
        
        <div class="nav-links" v-else>
          <router-link to="/auth/login" class="btn btn-primary">Login</router-link>
        </div>
      </nav>
    </header>
    
    <main class="app-main">
      <router-view />
    </main>
  </div>
</template>

<script setup>
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from './stores/auth'

const authStore = useAuthStore()
const router = useRouter()

onMounted(() => {
  authStore.initializeAuth()
})

function handleLogout() {
  authStore.logout()
  router.push('/auth/login')
}
</script>

<style scoped>
.app-header {
  background-color: var(--secondary-bg);
  border-bottom: 1px solid var(--border-color);
  position: sticky;
  top: 0;
  z-index: 100;
}

.nav-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 1rem;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.nav-brand {
  display: flex;
  align-items: center;
}

.brand-link {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  font-size: 1.5rem;
  font-weight: 700;
  color: var(--primary-text);
  transition: var(--transition);
}

.brand-link:hover {
  color: var(--accent-color);
}

.brand-text {
  font-size: 1.5rem;
  font-weight: 700;
}

.nav-links {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.nav-link {
  color: var(--secondary-text);
  transition: var(--transition);
  padding: 0.5rem 0.75rem;
  border-radius: 0.375rem;
}

.nav-link:hover {
  color: var(--primary-text);
  background-color: var(--tertiary-bg);
}

.nav-link.router-link-active {
  color: var(--accent-color);
  background-color: rgba(59, 130, 246, 0.1);
}

.nav-btn {
  padding: 0.5rem 1rem;
}

.app-main {
  min-height: calc(100vh - 5rem);
  padding: 2rem 0;
}

/* Responsive adjustments */
@media (max-width: 768px) {
  .nav-container {
    flex-direction: column;
    gap: 1rem;
  }
  
  .nav-links {
    width: 100%;
    justify-content: center;
    flex-wrap: wrap;
  }
  
  .brand-text {
    font-size: 1.25rem;
  }
}
</style>