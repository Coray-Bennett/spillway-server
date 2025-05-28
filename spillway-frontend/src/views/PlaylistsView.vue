<template>
  <div class="playlists-view">
    <div class="container">
      <header class="page-header">
        <div class="header-content">
          <h1 class="page-title">Playlists</h1>
          <router-link to="/upload" class="btn btn-primary">
            <BaseIcon name="plus" :size="16" />
            Create Playlist
          </router-link>
        </div>
      </header>
      
      <div v-if="isLoading" class="loading-spinner">
        Loading playlists...
      </div>
      
      <div v-else-if="playlists.length === 0" class="empty-state">
        <BaseIcon name="folder" :size="64" class="empty-icon" />
        <h2 class="empty-title">No playlists yet</h2>
        <p class="empty-text">Start by creating your first playlist</p>
        <router-link to="/upload" class="btn btn-primary">
          Create Playlist
        </router-link>
      </div>
      
      <div v-else class="playlists-grid grid grid-2">
        <div v-for="playlist in playlistsArr" :key="playlist.id" class="playlist-card card">
          <div class="playlist-header">
            <h3 class="playlist-title">{{ playlist.name }}</h3>
            <button @click="togglePlaylist(playlist.id)" class="btn btn-secondary btn-icon">
              <BaseIcon 
                :name="expandedPlaylists.has(playlist.id) ? 'collapse' : 'expand'" 
                :size="20"
              />
            </button>
          </div>
          
          <p v-if="playlist.description" class="playlist-description">
            {{ playlist.description }}
          </p>
          
          <div class="playlist-meta">
            <span class="meta-item">
              <span class="meta-value">{{ playlist.videos?.length || 0 }}</span>
              {{ playlist.videos?.length === 1 ? 'video' : 'videos' }}
            </span>
            <span class="meta-item">
              Created: {{ formatDate(playlist.createdAt) }}
            </span>
          </div>
          
          <div
            v-if="expandedPlaylists.has(playlist.id)"
            class="playlist-videos"
          >
            <h4 class="videos-title">Videos in Playlist</h4>
            <div v-if="playlistVideos[playlist.id]?.length" class="videos-list">
              <router-link
                v-for="video in playlistVideos[playlist.id]"
                :key="video.id"
                :to="`/video/${video.id}`"
                class="video-item"
              >
                <div class="video-number">
                  <span v-if="video.type === 'EPISODE'">
                    S{{ video.seasonNumber }}E{{ video.episodeNumber }}
                  </span>
                  <span v-else>
                    {{ playlistVideos[playlist.id].indexOf(video) + 1 }}
                  </span>
                </div>
                <div class="video-content">
                  <h5 class="video-title">{{ video.title }}</h5>
                  <div class="video-info">
                    <span>{{ formatDuration(video.length) }}</span>
                    <span :class="['video-status', video.conversionStatus?.toLowerCase()]">
                      {{ formatStatus(video.conversionStatus) }}
                    </span>
                  </div>
                </div>
              </router-link>
            </div>
            <p v-else class="no-videos">No videos in this playlist yet</p>
          </div>
        </div>
      </div>
      
      <div v-if="error" class="error-text">
        {{ error }}
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, reactive } from 'vue'
import { useVideoStore } from '../stores/video'
import BaseIcon from '../components/icons/BaseIcon.vue'

const videoStore = useVideoStore()
const playlists = ref(new Map())
const playlistsArr = ref([])
const isLoading = ref(false)
const error = ref('')
const expandedPlaylists = ref(new Set())
const playlistVideos = reactive({})

onMounted(async () => {
  try {
    isLoading.value = true
    await videoStore.getMyPlaylists()
    for(const playlist of videoStore.playlists) {
      playlists.value.set(playlist.id, playlist)
    }
    playlistsArr.value = videoStore.playlists
  } catch (err) {
    error.value = 'Failed to load playlists'
    console.error(err)
  } finally {
    isLoading.value = false
  }
})

async function togglePlaylist(playlistId) {
  if (expandedPlaylists.value.has(playlistId)) {
    expandedPlaylists.value.delete(playlistId)
  } else {
    expandedPlaylists.value.add(playlistId)
    
    if (!playlistVideos[playlistId]) {
      try {
        const videos = playlists.value.get(playlistId).videos
        playlistVideos[playlistId] = videos
      } catch (err) {
        console.error('Failed to load playlist videos:', err)
      }
    }
  }
}

function formatDate(dateArr) {
  if (!dateArr) return ''
  
  let year, month, day, etc
  [year, month, day, ...etc] = dateArr

  const dateString = `${year}/${month}/${day}`

  const date = new Date(dateString)
  return date.toLocaleDateString('en-US', {
    year: 'numeric',
    month: 'short',
    day: 'numeric'
  })
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
</script>

<style scoped>
.empty-icon {
  color: var(--accent-color);
  opacity: 0.5;
  margin-bottom: 1rem;
}

.header-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.playlist-card {
  overflow: hidden;
}

.playlist-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.75rem;
}

.playlist-title {
  font-size: 1.25rem;
  font-weight: 600;
}

.playlist-description {
  color: var(--secondary-text);
  margin-bottom: 1rem;
  line-height: 1.6;
}

.playlist-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  color: var(--secondary-text);
  font-size: 0.875rem;
  margin-bottom: 1.5rem;
  padding-top: 1rem;
  border-top: 1px solid var(--border-color);
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.meta-value {
  color: var(--primary-text);
  font-weight: 600;
}

.playlist-videos {
  margin-top: 1.5rem;
  padding-top: 1.5rem;
  border-top: 1px solid var(--border-color);
}

.videos-title {
  font-size: 1rem;
  font-weight: 600;
  margin-bottom: 1rem;
  color: var(--secondary-text);
}

.videos-list {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.video-item {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 0.75rem;
  background-color: var(--tertiary-bg);
  border-radius: 0.5rem;
  transition: var(--transition);
  color: inherit;
}

.video-item:hover {
  background-color: rgba(59, 130, 246, 0.1);
  border-color: var(--accent-color);
}

.video-number {
  flex-shrink: 0;
  width: 48px;
  height: 32px;
  background-color: var(--secondary-bg);
  border-radius: 0.375rem;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--primary-text);
  font-weight: 600;
  font-size: 0.875rem;
}

.video-content {
  flex: 1;
  min-width: 0;
}

.video-title {
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.video-info {
  display: flex;
  align-items: center;
  gap: 1rem;
  color: var(--secondary-text);
  font-size: 0.8125rem;
  margin-top: 0.25rem;
}

.video-status {
  padding: 0.125rem 0.5rem;
  border-radius: 0.25rem;
  font-weight: 500;
}

.video-status.completed {
  background-color: rgba(16, 185, 129, 0.2);
  color: var(--success-color);
}

.video-status.in_progress {
  background-color: rgba(59, 130, 246, 0.2);
  color: var(--accent-color);
}

.video-status.pending {
  background-color: rgba(148, 163, 184, 0.2);
  color: #94a3b8;
}

.video-status.failed {
  background-color: rgba(239, 68, 68, 0.2);
  color: var(--danger-color);
}

.no-videos {
  text-align: center;
  color: var(--secondary-text);
  font-style: italic;
  margin-top: 1rem;
}

.empty-state {
  text-align: center;
  max-width: 500px;
  margin: 4rem auto;
}

.empty-icon {
  font-size: 4rem;
  margin-bottom: 1rem;
}

.empty-title {
  font-size: 1.5rem;
  margin-bottom: 0.5rem;
}

.empty-text {
  color: var(--secondary-text);
  margin-bottom: 2rem;
}

.btn-icon {
  width: 32px;
  height: 32px;
  padding: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: normal;
  font-size: 1.25rem;
}

@media (max-width: 768px) {
  .header-content {
    flex-direction: column;
    align-items: flex-start;
    gap: 1rem;
  }
  
  .playlists-grid {
    grid-template-columns: 1fr;
  }
}
</style>