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
          <h1 class="video-title">{{ videoMetadata?.title || videoId }}</h1>
          <p v-if="!videoMetadata">Playlist URL: {{ playlistUrl }}</p>
          
          <div v-if="videoMetadata" class="video-meta">
            <span class="meta-item">
              <strong>Type:</strong> {{ videoMetadata.type }}
            </span>
            <span class="meta-item">
              <strong>Genre:</strong> {{ videoMetadata.genre || 'N/A' }}
            </span>
            <span class="meta-item">
              <strong>Duration:</strong> {{ formatDuration(videoMetadata.length) }}
            </span>
            <span v-if="videoMetadata.type === 'EPISODE'" class="meta-item">
              <strong>Season:</strong> {{ videoMetadata.seasonNumber || 'N/A' }}
            </span>
            <span v-if="videoMetadata.type === 'EPISODE'" class="meta-item">
              <strong>Episode:</strong> {{ videoMetadata.episodeNumber || 'N/A' }}
            </span>
          </div>
          
          <div v-if="videoMetadata?.description" class="video-description">
            <h3>Description</h3>
            <p>{{ videoMetadata.description }}</p>
          </div>
          
          <div class="video-info">
            <h3>Video Information</h3>
            <div class="info-grid">
              <div class="info-item">
                <span class="info-label">Status:</span>
                <span :class="['status-badge', videoMetadata?.conversionStatus?.toLowerCase()]">
                  {{ formatStatus(videoMetadata?.conversionStatus) }}
                </span>
              </div>
              <div v-if="videoMetadata?.createdAt" class="info-item">
                <span class="info-label">Created:</span>
                <span>{{ formatDate(videoMetadata.createdAt) }}</span>
              </div>
              <div v-if="videoMetadata?.updatedAt" class="info-item">
                <span class="info-label">Last Updated:</span>
                <span>{{ formatDate(videoMetadata.updatedAt) }}</span>
              </div>
            </div>
          </div>
          
          <button @click="loadVideo" class="btn btn-primary">
            {{ videoMetadata ? 'Reload Video' : 'Load Video' }}
          </button>
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
const videoMetadata = ref(null)

let hls = null
let playlistUrl = ref(`http://localhost:8081/video/${videoId.value}/playlist`)

onUnmounted(() => {
  if (hls) {
    hls.destroy()
    hls = null
  }
})

async function fetchVideoMetadata() {
  try {
    const response = await fetch(`http://localhost:8081/video/${videoId.value}`)
    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`)
    }
    videoMetadata.value = await response.json()
    // Update playlist URL from metadata if available
    if (videoMetadata.value?.playlistUrl) {
      playlistUrl.value = videoMetadata.value.playlistUrl
    }
    console.log('Video metadata loaded:', videoMetadata.value)
  } catch (err) {
    console.error('Failed to fetch video metadata:', err)
    error.value = `Failed to load video metadata: ${err.message}`
  }
}

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

function formatDuration(seconds) {
  if (!seconds) return '0:00'
  const hours = Math.floor(seconds / 3600)
  const minutes = Math.floor((seconds % 3600) / 60)
  const remainingSeconds = seconds % 60
  
  if (hours > 0) {
    return `${hours}:${minutes.toString().padStart(2, '0')}:${remainingSeconds.toString().padStart(2, '0')}`
  } else {
    return `${minutes}:${remainingSeconds.toString().padStart(2, '0')}`
  }
}

function formatStatus(status) {
  if (!status) return 'Unknown'
  const statusMap = {
    'PENDING': 'Pending',
    'IN_PROGRESS': 'Converting',
    'COMPLETED': 'Ready',
    'FAILED': 'Failed',
    'CANCELLED': 'Cancelled'
  }
  return statusMap[status] || status
}

function formatDate(dateString) {
  if (!dateString) return 'N/A'
  return new Date(dateString).toLocaleString()
}

// Load metadata and video on mount
onMounted(async () => {
  await fetchVideoMetadata()
  // Only auto-load video if it's completed
  if (videoMetadata.value?.conversionStatus === 'COMPLETED') {
    setTimeout(loadVideo, 100) // Small delay to ensure DOM is ready
  }
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
  font-size: 2rem;
  font-weight: 700;
  line-height: 1.3;
  margin-bottom: 1.5rem;
  color: var(--primary-text);
}

.video-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 1.5rem;
  margin-bottom: 2rem;
  color: var(--secondary-text);
}

.meta-item {
  font-size: 0.9375rem;
}

.video-description,
.video-info {
  margin-top: 2rem;
}

.video-description h3,
.video-info h3 {
  font-size: 1.25rem;
  font-weight: 600;
  margin-bottom: 1rem;
  color: var(--primary-text);
}

.video-description p {
  color: var(--secondary-text);
  line-height: 1.7;
}

.info-grid {
  display: grid;
  gap: 1rem;
}

.info-item {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.info-label {
  width: 120px;
  color: var(--secondary-text);
}

.status-badge {
  padding: 0.25rem 0.75rem;
  border-radius: 9999px;
  font-size: 0.875rem;
  font-weight: 500;
  text-transform: capitalize;
}

.status-badge.pending {
  background-color: rgba(148, 163, 184, 0.2);
  color: #94a3b8;
}

.status-badge.in_progress {
  background-color: rgba(59, 130, 246, 0.2);
  color: var(--accent-color);
}

.status-badge.completed {
  background-color: rgba(16, 185, 129, 0.2);
  color: var(--success-color);
}

.status-badge.failed {
  background-color: rgba(239, 68, 68, 0.2);
  color: var(--danger-color);
}

.status-badge.cancelled {
  background-color: rgba(107, 114, 128, 0.2);
  color: #6b7280;
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
  font-size: 0.9375rem;
}

.btn-primary {
  background-color: var(--accent-color);
  color: white;
}

.btn-primary:hover {
  background-color: var(--accent-hover);
}

@media (max-width: 768px) {
  .video-meta {
    flex-direction: column;
    gap: 0.5rem;
  }
  
  .info-label {
    width: auto;
  }
}
</style>