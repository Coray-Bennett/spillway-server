<template>
  <div class="video-view">
    <div class="container">
      <div v-if="error" class="error-state">
        <BaseIcon name="error" :size="64" class="error-icon" />
        <h2 class="error-title">Error</h2>
        <p class="error-text">{{ error }}</p>
        
        <div v-if="isEncryptionError || isManifestError" class="error-actions">
          <p class="error-hint">
            <BaseIcon name="info" :size="16" />
            This may be due to an incorrect encryption key or permission issue.
          </p>
          
          <div class="error-buttons">
            <button @click="showEncryptionKeyModal = true" class="btn btn-primary">
              <BaseIcon name="key" :size="16" />
              Enter Encryption Key
            </button>
            
            <router-link to="/encryption-manager" class="btn btn-secondary">
              <BaseIcon name="settings" :size="16" />
              Manage Keys
            </router-link>
          </div>
        </div>
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

          <div class="video-actions">
            <div v-if="isOwner" class="owner-actions">
              <button @click="showEditModal = true" class="btn btn-secondary">
                <BaseIcon name="edit" :size="16" />
                Edit
              </button>
              <button @click="showSharingModal = true" class="btn btn-outline">
                <BaseIcon name="share" :size="16" />
                Share Video
              </button>
            </div>
            <!-- Encryption key management button for encrypted videos -->
            <button 
              v-if="videoMetadata?.encrypted" 
              @click="showKeyManagementModal = true" 
              class="btn btn-outline"
            >
              <BaseIcon name="key" :size="16" />
              Manage Encryption Key
            </button>
          </div>
          
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
            <span v-if="videoMetadata.encrypted" class="meta-item encrypted-badge">
              <BaseIcon name="lock" :size="14" />
              <strong>Encrypted</strong>
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

          <PlaylistManager 
            v-if="videoMetadata && isOwner" 
            :video="videoMetadata" 
            @updated="fetchVideoMetadata"
          />
        </div>
      </div>

      <VideoEditModal
        v-if="showEditModal && videoMetadata"
        :video="videoMetadata"
        @close="showEditModal = false"
        @updated="onVideoUpdated"
      />

      <VideoSharingModal
        v-if="showSharingModal && videoMetadata"
        :video="videoMetadata"
        @close="showSharingModal = false"
        @shared="onVideoShared"
      />

      <!-- Encryption Key Modal -->
      <EncryptionKeyModal
        v-if="showEncryptionKeyModal"
        :video-id="videoId"
        :video-title="videoMetadata?.title"
        @close="showEncryptionKeyModal = false"
        @submit="onEncryptionKeySubmit"
      />

      <!-- Key Management Modal -->
      <KeyManagementModal
        v-if="showKeyManagementModal"
        :video-id="videoId"
        :video-title="videoMetadata?.title"
        :current-key="encryptionKey"
        @close="showKeyManagementModal = false"
        @update="onKeyUpdate"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { useVideoStore } from '../stores/video'
import { formatDate } from '@/utils/date'
import { formatDuration } from '@/utils/metadata'
import Hls from 'hls.js'
import VideoEditModal from '../components/VideoEditModal.vue'
import VideoSharingModal from '../components/VideoSharingModal.vue'
import PlaylistManager from '../components/PlaylistManager.vue'
import EncryptionKeyModal from '../components/EncryptionKeyModal.vue'
import KeyManagementModal from '../components/KeyManagementModal.vue'
import BaseIcon from '../components/icons/BaseIcon.vue'
import encryptionKeyService from '@/services/encryptionKeyService'

const route = useRoute()
const router = useRouter()
const videoId = ref(route.params.id)
const videoPlayer = ref(null)
const error = ref('')
const videoMetadata = ref(null)
const showEditModal = ref(false)
const showSharingModal = ref(false)
const showEncryptionKeyModal = ref(false)
const showKeyManagementModal = ref(false)
const authStore = useAuthStore()
const videoStore = useVideoStore()
const encryptionKey = ref(null)

const isOwner = computed(() => {
  if (!authStore.isAuthenticated || !videoMetadata.value) return false
  return videoMetadata.value.uploadedBy?.username === authStore.currentUsername
})

const isEncryptionError = computed(() => {
  return error.value && (
    error.value.toLowerCase().includes('decrypt') || 
    error.value.toLowerCase().includes('encryption') ||
    error.value.toLowerCase().includes('decryption')
  )
})

const isManifestError = computed(() => {
  return error.value && (
    error.value.toLowerCase().includes('manifest') ||
    error.value.toLowerCase().includes('playlist') ||
    error.value.toLowerCase().includes('403') ||
    error.value.toLowerCase().includes('401') ||
    error.value.toLowerCase().includes('forbidden') ||
    error.value.toLowerCase().includes('unauthorized')
  )
})

function onVideoUpdated(updatedVideo) {
  videoMetadata.value = updatedVideo
  showEditModal.value = false
}

function onVideoShared(share) {
  console.log('Video shared successfully:', share)
  showSharingModal.value = false
}

function onEncryptionKeySubmit(key) {
  encryptionKey.value = key
  showEncryptionKeyModal.value = false
  loadVideo()
}

function onKeyUpdate(newKey) {
  encryptionKey.value = newKey
  if (newKey) {
    encryptionKeyService.storeKey(videoId.value, newKey)
  }
  showKeyManagementModal.value = false
  loadVideo()
}

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
    const data = await videoStore.getVideo(videoId.value)
    videoMetadata.value = data

    if(data?.encrypted) {
      // First check if we have a stored key for this video
      const storedKey = encryptionKeyService.getKey(videoId.value)
      
      if (storedKey) {
        console.log('Found stored encryption key for video')
        encryptionKey.value = storedKey
      } else {
        // Show modal to request encryption key
        showEncryptionKeyModal.value = true
      }
    }
    
    // Update playlist URL from metadata if available
    if (data?.playlistUrl) {
      playlistUrl.value = data.playlistUrl
    }
    console.log('Video metadata loaded:', data)
    console.log('Current user:', authStore.currentUsername)
    console.log('Video uploader:', data.uploadedBy?.username)
    console.log('Is owner:', isOwner.value)
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

  // Check if encrypted video has a key
  if (videoMetadata.value?.encrypted && !encryptionKey.value) {
    showEncryptionKeyModal.value = true
    return
  }
  
  error.value = ''
  
  console.log('Loading video with URL:', playlistUrl.value)
  
  if (Hls.isSupported()) {
    console.log('HLS.js is supported')
    
    if (hls) {
      hls.destroy()
    }
    
    hls = new Hls({
      debug: true,
      xhrSetup: xhr => {
        xhr.setRequestHeader('Authorization', `Bearer ${authStore.token}`)
        if (encryptionKey.value) {
          xhr.setRequestHeader('X-Decryption-Key', encryptionKey.value)
        }
      }
    })
    
    hls.loadSource(playlistUrl.value)
    hls.attachMedia(videoPlayer.value)
    
    hls.on(Hls.Events.MANIFEST_PARSED, () => {
      console.log('Manifest parsed')
      videoPlayer.value.play()
    })
    
    hls.on(Hls.Events.ERROR, (event, data) => {
      console.error('HLS error:', data)
      
      const isEncryptionIssue = 
        data.details && (
          data.details.includes('decrypt') || 
          data.details.includes('key')
        )
      
      const isNetworkIssue = 
        data.response && (
          data.response.code === 401 || 
          data.response.code === 403
        )
      
      if (data.fatal) {
        if (isEncryptionIssue) {
          error.value = `Decryption Error: The encryption key appears to be incorrect.`
          showEncryptionKeyModal.value = true
        } else if (isNetworkIssue) {
          error.value = `Access Error (${data.response.code}): You don't have permission to access this video or the encryption key is incorrect.`
        } else {
          error.value = `Playback Error: ${data.type} - ${data.details}`
        }
      }
      
      // Even for non-fatal errors, if it's clearly an encryption issue, show the key modal
      if (!data.fatal && isEncryptionIssue) {
        showEncryptionKeyModal.value = true
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

onMounted(async () => {
  await fetchVideoMetadata()
  if (videoMetadata.value?.conversionStatus === 'COMPLETED') {
    // Auto-load if we have a stored key or video is not encrypted
    if (!videoMetadata.value.encrypted || encryptionKey.value) {
      setTimeout(loadVideo, 100)
    }
  }
})
</script>

<style scoped>
.error-icon {
  color: var(--danger-color);
  margin-bottom: 1rem;
}

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
  max-width: 600px;
  padding: 2rem;
  background-color: var(--secondary-bg);
  border-radius: 0.75rem;
  border: 1px solid var(--border-color);
}

.error-icon {
  font-size: 3rem;
  margin-bottom: 1rem;
  color: var(--danger-color);
}

.error-title {
  font-size: 1.5rem;
  margin-bottom: 0.5rem;
  color: var(--primary-text);
}

.error-text {
  color: var(--secondary-text);
  margin-bottom: 1.5rem;
}

.error-actions {
  margin-top: 1.5rem;
  padding-top: 1.5rem;
  border-top: 1px solid var(--border-color);
}

.error-hint {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  color: var(--accent-color);
  margin-bottom: 1.5rem;
  font-size: 0.9375rem;
}

.error-buttons {
  display: flex;
  gap: 1rem;
  justify-content: center;
}

@media (max-width: 600px) {
  .error-buttons {
    flex-direction: column;
  }
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

.encrypted-badge {
  display: flex;
  align-items: center;
  gap: 0.25rem;
  color: var(--accent-color);
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

.video-actions {
  margin-bottom: 2rem;
  display: flex;
  gap: 1rem;
  flex-wrap: wrap;
}

.owner-actions {
  display: flex;
  gap: 1rem;
  flex-wrap: wrap;
}

.btn {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.75rem 1rem;
  border-radius: 0.5rem;
  font-weight: 500;
  text-decoration: none;
  border: none;
  cursor: pointer;
  transition: all 0.2s;
  font-size: 0.875rem;
}

.btn-primary {
  background-color: var(--accent-color);
  color: white;
}

.btn-primary:hover {
  background-color: var(--accent-hover);
}

.btn-secondary {
  background-color: var(--secondary-bg);
  color: var(--primary-text);
  border: 1px solid var(--border-color);
}

.btn-secondary:hover {
  background-color: var(--hover-bg);
}

.btn-outline {
  background-color: transparent;
  color: var(--accent-color);
  border: 1px solid var(--accent-color);
}

.btn-outline:hover {
  background-color: var(--accent-color);
  color: white;
}

.btn-danger {
  background-color: var(--danger-color);
  color: white;
}

.btn-danger:hover {
  opacity: 0.9;
}

@media (max-width: 768px) {
  .video-meta {
    flex-direction: column;
    gap: 0.5rem;
  }
  
  .info-label {
    width: auto;
  }
  
  .owner-actions,
  .video-actions {
    flex-direction: column;
    align-items: stretch;
  }

  .btn {
    justify-content: center;
  }
}
</style>