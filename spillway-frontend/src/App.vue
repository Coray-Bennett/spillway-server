<template>
  <div class="app">
    <!-- SVG Filter for wave distortion -->
    <svg style="position: absolute; width: 0; height: 0">
      <filter id="wave-filter">
        <feTurbulence 
          type="turbulence" 
          baseFrequency="0.02" 
          numOctaves="2" 
          seed="2">
          <animate 
            attributeName="baseFrequency" 
            dur="60s" 
            values="0.02;0.025;0.02" 
            repeatCount="indefinite" />
        </feTurbulence>
        <feDisplacementMap in="SourceGraphic" scale="5">
          <animate 
            attributeName="scale" 
            dur="45s" 
            values="5;8;5" 
            repeatCount="indefinite" />
        </feDisplacementMap>
      </filter>
    </svg>

    <div id="icon-sprite" v-html="iconSprite"></div>
    
    <header class="app-header">
      <nav class="nav-container">
        <div class="nav-brand">
          <router-link to="/" class="brand-link">
            <BaseIcon name="spillway" :size="32" />
            <span class="brand-text">Spillway</span>
          </router-link>
        </div>
        
        <div class="nav-links" v-if="authStore.isAuthenticated">
          <router-link to="/upload" class="nav-link">
            <BaseIcon name="upload" :size="16" />
            <span>Upload</span>
          </router-link>
          <router-link to="/videos" class="nav-link">
            <BaseIcon name="video" :size="16" />
            <span>My Videos</span>
          </router-link>
          <router-link to="/playlists" class="nav-link">
            <BaseIcon name="playlist" :size="16" />
            <span>Playlists</span>
          </router-link>
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
import BaseIcon from './components/icons/BaseIcon.vue'
import iconSprite from './assets/icons.xml?raw'

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
/* Add the lattice background to the app container */
 .app::before {
  content: '';
  position: fixed;
  inset: -50%;
  pointer-events: none;
  z-index: -1;
  opacity: 0.12;
  background: 
    repeating-linear-gradient(
      45deg,
      transparent,
      transparent 60px,
      var(--accent-color) 60px,
      var(--accent-color) 61px
    ),
    repeating-linear-gradient(
      -45deg,
      transparent,
      transparent 60px,
      var(--accent-color) 60px,
      var(--accent-color) 61px
    );
  filter: url('#wave-filter');
}

#icon-sprite {
  display: none;
}

.app-header {
  background-color: var(--secondary-bg);
  border-bottom: 1px solid var(--border-color);
  position: sticky;
  top: 0;
  z-index: 100;
  backdrop-filter: blur(10px);
  background-color: rgba(20, 20, 20, 0.9);
}

.nav-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 1rem;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.brand-link {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  transition: var(--transition);
}

.brand-link:hover {
  transform: scale(1.05);
}

.brand-text {
  font-size: 1.5rem;
  font-weight: 800;
  background: linear-gradient(135deg, var(--primary-text), var(--accent-color));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.nav-links {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.nav-link {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  color: var(--secondary-text);
  transition: var(--transition);
  padding: 0.625rem 1rem;
  border-radius: 0.5rem;
  font-weight: 500;
}

.nav-link:hover {
  color: var(--accent-color);
  background-color: rgba(99, 102, 241, 0.1);
}

.nav-link.router-link-active {
  color: var(--accent-color);
  background-color: rgba(99, 102, 241, 0.15);
}

.app-main {
  min-height: calc(100vh - 5rem);
  padding: 2rem 0;
}

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
}
</style>