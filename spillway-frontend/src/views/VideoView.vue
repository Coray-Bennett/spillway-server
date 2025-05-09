<template>
  <div class="video-view">
    <div class="container">
      <div v-if="isLoading" class="loading-spinner">
        Loading video...
      </div>
      
      <div v-else-if="error" class="error-state">
        <div class="error-icon">❌</div>
        <h2 class="error-title">Video Not Found</h2>
        <p class="error-text">{{ error }}</p>
        <router-link to="/videos" class="btn btn-primary">
          Back to Videos
        </router-link>
      </div>
      
      <div v-else class="video-content">
        <div class="video-player-container">
          <div v-if="video.conversionStatus !== 'COMPLETED'" class="player-placeholder">
            <div v-if="video.conversionStatus === 'IN_PROGRESS'" class="conversion-progress">
              <div class="progress-circle">
                <span class="progress-value">{{ conversionProgress }}%</span>
              </div>
              <p class="progress-text">Converting video...</p>
            </div>
            <div v-else class="conversion-waiting">
              <div class="waiting-icon">⏳</div>
              <h3>{{ getConversionStatusMessage() }}</h3>
              <p>This video is not ready for playback yet.</p>
            </div>
          </div>
          
          <video
            v-else
            ref="videoPlayer"
            class="video-player"
            controls
            @loadedmetadata="onVideoLoaded"
          >
            <p>Your browser does not support the video tag.</p>
          </video>
        </div>
        
        <div class="video-details">
          <div class="video-header">
            <h1 class="video-title">{{ video.title }}</h1>
            <div class="video-actions">
              <button v-if="video.conversionStatus === 'COMPLETED'" 
                      @click="togglePictureInPicture" 
                      class="btn btn-secondary btn-icon">
                📺
              </button>
              <button v-if="video.conversionStatus === 'COMPLETED'" 
                      @click="toggleFullscreen" 
                      class="btn btn-secondary btn-icon">
                🔍
              </button>
            </div>
          </div>
          
          <div class="video-meta">
            <span class="meta-item">
              <strong>Type:</strong> {{ video.type }}
            </span>
            <span class="meta-item">
              <strong>Genre:</strong> {{ video.genre || 'N/A' }}
            </span>
            <span class="meta-item">
              <strong>Duration:</strong> {{ formatDuration(video.length) }}
            </span>
            <span v-if="video.type === 'EPISODE'" class="meta-item">
              <strong>Season:</strong> {{ video.seasonNumber || 'N/A' }}
            </span>
            <span v-if="video.type === 'EPISODE'" class="meta-item">
              <strong>Episode:</strong> {{ video.episodeNumber || 'N/A' }}
            </span>
          </div>
          
          <div v-if="video.description" class="video-description">
            <h3>Description</h3>
            <p>{{ video.description }}</p>
          </div>
          
          <div class="video-info">
            <h3>Video Information</h3>
            <div class="info-grid">
              <div class="info-item">
                <span class="info-label">Status:</span>
                <span :class="['status-badge', video.conversionStatus?.toLowerCase()]">
                  {{ formatStatus(video.conversionStatus) }}
                </span>
              </div>
              <div class="info-item">
                <span class="info-label">Created:</span>
                <span>{{ formatDate(video.createdAt) }}</span>
              </div>
              <div class="info-item">
                <span class="info-label">Last Updated:</span>
                <span>{{ formatDate(video.updatedAt) }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, computed } from 'vue'
import { useRoute } from 'vue-router'
import { useVideoStore } from '../stores/video'
import Hls from 'hls.js'

const route = useRoute()
const videoStore = useVideoStore()
const videoPlayer = ref(null)
const isLoading = ref(true)
const error = ref('')
const conversionProgress = ref(0)

let hls = null
let statusCheckInterval = null

const video = computed(() => videoStore.currentVideo)

onMounted(async () => {
  try {
    await videoStore.getVideo(route.params.id)
    
    if (video.value.conversionStatus === 'COMPLETED') {
      initializePlayer()
    } else {
      startStatusCheck()
    }
    
    isLoading.value = false
  } catch (err) {
    error.value = 'Failed to load video'
    isLoading.value = false
  }
})

onUnmounted(() => {
  if (hls) {
    hls.destroy()
  }
  if (statusCheckInterval) {
    clearInterval(statusCheckInterval)
  }
})

async function initializePlayer() {
  if (!videoPlayer.value) return
  
  const playlistUrl = video.value.playlistUrl
  
  if (Hls.isSupported()) {
    hls = new Hls()
    hls.loadSource(playlistUrl)
    hls.attachMedia(videoPlayer.value)
    
    hls.on(Hls.Events.MANIFEST_PARSED, () => {
      console.log('HLS manifest loaded')
    })
    
    hls.on(Hls.Events.ERROR, (event, data) => {
      console.error('HLS error:', data)
      if (data.fatal) {
        switch (data.type) {
          case Hls.ErrorTypes.NETWORK_ERROR:
            error.value = 'Network error loading video'
            break
          case Hls.ErrorTypes.MEDIA_ERROR:
            error.value = 'Media error playing video'
            break
          default:
            error.value = 'Error playing video'
            break
        }
      }
    })
  } else if (videoPlayer.value.canPlayType('application/vnd.apple.mpegurl')) {
    // Native HLS support (Safari)
    videoPlayer.value.src = playlistUrl
  } else {
    error.value = 'Your browser does not support HLS playback'
  }
}

function startStatusCheck() {
  statusCheckInterval = setInterval(async () => {
    const status = await videoStore.getVideoStatus(route.params.id)
    if (status) {
      conversionProgress.value = status.progress || 0
      
      if (status.status === 'COMPLETED') {
        clearInterval(statusCheckInterval)
        video.value.conversionStatus = 'COMPLETED'
        await initializePlayer()
      }
    }
  }, 2000)
}

function getConversionStatusMessage() {
  switch (video.value.conversionStatus) {
    case 'PENDING':
      return 'Video is queued for conversion'
    case 'IN_PROGRESS':
      return 'Video is being converted'
    case 'FAILED':
      return 'Video conversion failed'
    case 'CANCELLED':
      return 'Video conversion was cancelled'
    default:
      return 'Video is not ready for playback'
  }
}

function toggleFullscreen() {
  if (!videoPlayer.value) return
  
  if (!document.fullscreenElement) {
    videoPlayer.value.requestFullscreen()
  } else {
    document.exitFullscreen()
  }
}

function togglePictureInPicture() {
  if (!videoPlayer.value) return
  
  if (!document.pictureInPictureElement) {
    videoPlayer.value.requestPictureInPicture()
  } else {
    document.exitPictureInPicture()
  }
}

function onVideoLoaded() {
  console.log('Video loaded')
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
</script>

<style scoped>
.video-content {
  max-width: 1200px;
  margin: 0 auto;
}

.video-player-container {
  position: relative;
  width: 100%;
  padding-bottom: 56.25%; /* 16:9 aspect ratio */
  margin-bottom: 2rem;
  background-color: var(--tertiary-bg);
  border-radius: 0.75rem;
  overflow: hidden;
}

.video-player,
.player-placeholder {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
}

.video-player {
  background-color: #000;
}

.player-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: var(--secondary-bg);
}

.conversion-progress {
  text-align: center;
}

.progress-circle {
  width: 100px;
  height: 100px;
  border-radius: 50%;
  border: 3px solid var(--border-color);
  border-top-color: var(--accent-color);
  animation: spin 1s linear infinite;
  position: relative;
  margin: 0 auto 1rem;
}

.progress-value {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  font-weight: 600;
  color: var(--primary-text);
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.progress-text {
  color: var(--secondary-text);
  margin-top: 1rem;
}

.conversion-waiting {
  text-align: center;
  max-width: 400px;
  padding: 2rem;
}

.waiting-icon {
  font-size: 3rem;
  margin-bottom: 1rem;
}

.video-details {
  background-color: var(--secondary-bg);
  border-radius: 0.75rem;
  padding: 2rem;
  border: 1px solid var(--border-color);
}

.video-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 1.5rem;
}

.video-title {
  font-size: 2rem;
  font-weight: 700;
  line-height: 1.3;
}

.video-actions {
  display: flex;
  gap: 0.5rem;
}

.btn-icon {
  width: 40px;
  height: 40px;
  padding: 0;
  display: flex;
  align-items: center;
  justify-content: center;
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

.error-state {
  text-align: center;
  max-width: 500px;
  margin: 4rem auto;
}

.error-icon {
  font-size: 3rem;
  margin-bottom: 1rem;
}

.error-title {
  font-size: 1.5rem;
  margin-bottom: 0.5rem;
}

.error-text {
  color: var(--secondary-text);
  margin-bottom: 2rem;
}

@media (max-width: 768px) {
  .video-header {
    flex-direction: column;
    gap: 1rem;
  }
  
  .video-actions {
    align-self: flex-start;
  }
  
  .video-meta {
    flex-direction: column;
    gap: 0.5rem;
  }
  
  .info-label {
    width: auto;
  }
}
</style>