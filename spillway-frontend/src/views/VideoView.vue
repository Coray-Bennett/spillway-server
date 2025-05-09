<template>
  <div class="video-view">
    <div class="container">
      <div v-if="error" class="error-state">
        <div class="error-icon">❌</div>
        <h2 class="error-title">Error</h2>
        <p class="error-text">{{ error }}</p>
      </div>
      
      <div v-else class="video-content">
        <div class="video-player-container">
          <video
            ref="videoPlayer"
            class="video-player"
            controls
            @error="onVideoError"
          >
            Your browser does not support video playback.
          </video>
        </div>
        
        <div class="video-details">
          <h1 class="video-title">{{ videoId }}</h1>
          <p>Playlist URL: {{ playlistUrl }}</p>
          <button @click="loadVideo" class="btn btn-primary">Load Video</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import Hls from 'hls.js'

const route = useRoute()
const videoId = ref(route.params.id)
const videoPlayer = ref(null)
const error = ref('')
const playlistUrl = ref(`http://localhost:8081/video/${videoId.value}/playlist`)

let hls = null

onUnmounted(() => {
  if (hls) {
    hls.destroy()
    hls = null
  }
})

function loadVideo() {
  if (!videoPlayer.value) {
    error.value = 'Video player not found'
    return
  }
  
  // Clear any existing error
  error.value = ''
  
  console.log('Loading video with URL:', playlistUrl.value)
  
  if (Hls.isSupported()) {
    console.log('HLS.js is supported')
    
    if (hls) {
      hls.destroy()
    }
    
    hls = new Hls({
      debug: true
    })
    
    hls.loadSource(playlistUrl.value)
    hls.attachMedia(videoPlayer.value)
    
    hls.on(Hls.Events.MANIFEST_PARSED, () => {
      console.log('Manifest parsed')
      videoPlayer.value.play()
    })
    
    hls.on(Hls.Events.ERROR, (event, data) => {
      console.error('HLS error:', data)
      if (data.fatal) {
        error.value = `HLS Error: ${data.type} - ${data.details}`
      }
    })
  } else if (videoPlayer.value.canPlayType('application/vnd.apple.mpegurl')) {
    console.log('Native HLS support detected')
    videoPlayer.value.src = playlistUrl.value
    videoPlayer.value.load()
  } else {
    error.value = 'HLS is not supported in this browser'
  }
}

function onVideoError(event) {
  console.error('Video element error:', event)
  const videoError = event.target.error
  if (videoError) {
    error.value = `Video Error: Code ${videoError.code}`
  }
}

// Auto-load on mount
onMounted(() => {
  setTimeout(loadVideo, 100) // Small delay to ensure DOM is ready
})
</script>

<style scoped>
.video-view {
  padding: 2rem 0;
  min-height: 100vh;
  background-color: var(--primary-bg);
}

.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 1rem;
}

.error-state {
  text-align: center;
  margin: 2rem auto;
  max-width: 500px;
  padding: 2rem;
  background-color: var(--secondary-bg);
  border-radius: 0.75rem;
  border: 1px solid var(--border-color);
}

.error-icon {
  font-size: 3rem;
  margin-bottom: 1rem;
}

.error-title {
  font-size: 1.5rem;
  margin-bottom: 0.5rem;
  color: var(--primary-text);
}

.error-text {
  color: var(--secondary-text);
}

.video-content {
  max-width: 1200px;
  margin: 0 auto;
}

.video-player-container {
  position: relative;
  width: 100%;
  padding-bottom: 56.25%; /* 16:9 aspect ratio */
  margin-bottom: 2rem;
  background-color: #000;
  border-radius: 0.75rem;
  overflow: hidden;
}

.video-player {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: #000;
}

.video-details {
  background-color: var(--secondary-bg);
  border-radius: 0.75rem;
  padding: 2rem;
  border: 1px solid var(--border-color);
}

.video-title {
  font-size: 1.5rem;
  margin-bottom: 1rem;
  color: var(--primary-text);
}

.btn {
  display: inline-block;
  padding: 0.5rem 1rem;
  border-radius: 0.5rem;
  font-weight: 500;
  text-decoration: none;
  border: none;
  cursor: pointer;
  transition: all 0.2s;
  margin-top: 1rem;
}

.btn-primary {
  background-color: var(--accent-color);
  color: white;
}

.btn-primary:hover {
  background-color: var(--accent-hover);
}
</style>