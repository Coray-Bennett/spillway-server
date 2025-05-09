<template>
  <div class="home">
    <div class="container">
      <div class="hero">
        <h1 class="hero-title">Welcome to Spillway</h1>
        <p class="hero-subtitle">Your personal video streaming platform</p>
        
        <div class="hero-actions" v-if="!authStore.isAuthenticated">
          <router-link to="/auth/register" class="btn btn-primary">
            Get Started
          </router-link>
          <router-link to="/auth/login" class="btn btn-secondary">
            Sign In
          </router-link>
        </div>
        
        <div class="hero-actions" v-else>
          <router-link to="/upload" class="btn btn-primary">
            Upload Video
          </router-link>
          <router-link to="/videos" class="btn btn-secondary">
            Browse Videos
          </router-link>
        </div>
      </div>
      
      <div class="features" v-if="!authStore.isAuthenticated">
        <h2 class="section-title">Features</h2>
        <div class="grid grid-3">
          <div class="feature-card card">
            <div class="feature-icon">📹</div>
            <h3 class="feature-title">Upload Videos</h3>
            <p class="feature-desc">Upload your videos and convert them to HLS for streaming</p>
          </div>
          
          <div class="feature-card card">
            <div class="feature-icon">📝</div>
            <h3 class="feature-title">Create Playlists</h3>
            <p class="feature-desc">Organize your videos into seasons and episodes</p>
          </div>
          
          <div class="feature-card card">
            <div class="feature-icon">▶️</div>
            <h3 class="feature-title">Stream Anywhere</h3>
            <p class="feature-desc">Watch your content on any device with HLS streaming</p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { useAuthStore } from '../stores/auth'

const authStore = useAuthStore()
</script>

<style scoped>
.hero {
  text-align: center;
  max-width: 800px;
  margin: 4rem auto;
}

.hero-title {
  font-size: 3rem;
  margin-bottom: 1rem;
  background: linear-gradient(to right, var(--primary-text), var(--accent-color));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.hero-subtitle {
  font-size: 1.25rem;
  color: var(--secondary-text);
  margin-bottom: 2rem;
}

.hero-actions {
  display: flex;
  gap: 1rem;
  justify-content: center;
}

.features {
  margin-top: 6rem;
}

.section-title {
  text-align: center;
  font-size: 2rem;
  margin-bottom: 2rem;
}

.feature-card {
  text-align: center;
}

.feature-icon {
  font-size: 2rem;
  margin-bottom: 1rem;
}

.feature-title {
  font-size: 1.25rem;
  margin-bottom: 0.5rem;
}

.feature-desc {
  color: var(--secondary-text);
  line-height: 1.6;
}

@media (max-width: 768px) {
  .hero-title {
    font-size: 2rem;
  }
  
  .hero-actions {
    flex-direction: column;
    align-items: center;
  }
}
</style>