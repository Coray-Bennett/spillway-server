<template>
    <div class="videos-view">
      <div class="container">
        <header class="page-header">
          <div class="header-content">
            <h1 class="page-title">My Videos</h1>
            <router-link to="/upload" class="btn btn-primary">
              <span>+</span> Upload Video
            </router-link>
          </div>
        </header>
        
        <div v-if="isLoading" class="loading-spinner">
          Loading videos...
        </div>
        
        <div v-else-if="videos.length === 0" class="empty-state">
          <div class="empty-icon">📹</div>
          <h2 class="empty-title">No videos yet</h2>
          <p class="empty-text">Start by uploading your first video</p>
          <router-link to="/upload" class="btn btn-primary">
            Upload Video
          </router-link>
        </div>
        
        <div v-else class="videos-grid grid grid-3">
          <div v-for="video in videos" :key="video.id" class="video-card card">
            <div class="video-thumbnail">
              <div class="thumbnail-placeholder">
                <span class="video-type-badge">{{ video.type }}</span>
              </div>
              <div class="video-overlay">
                <router-link :to="`/video/${video.id}`" class="play-button">
                  ▶️
                </router-link>
              </div>
            </div>
            
            <div class="video-info">
              <h3 class="video-title">{{ video.title }}</h3>
              <div class="video-meta">
                <span class="video-genre">{{ video.genre || 'No genre' }}</span>
                <span class="video-duration">{{ formatDuration(video.length) }}</span>
              </div>
              <div class="video-status">
                <span 
                  :class="['status-badge', video.conversionStatus?.toLowerCase()]"
                >
                  {{ formatStatus(video.conversionStatus) }}
                </span>
                <span v-if="video.conversionStatus === 'IN_PROGRESS' && video.conversionProgress" 
                      class="progress-text">
                  {{ video.conversionProgress }}%
                </span>
              </div>
              
              <router-link :to="`/video/${video.id}`" class="btn btn-secondary btn-sm">
                View Details
              </router-link>
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
  import { ref, onMounted } from 'vue'
  import { useVideoStore } from '../stores/video'
  
  const videoStore = useVideoStore()
  const videos = ref([])
  const isLoading = ref(false)
  const error = ref('')
  
  onMounted(async () => {
    // Note: You'll need to implement a getMyVideos method in the video store
    // For now, we'll use the existing videos array from the store
    videos.value = videoStore.videos
  })
  
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
  .header-content {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
  
  .video-card {
    overflow: hidden;
    transition: var(--transition);
  }
  
  .video-card:hover {
    transform: translateY(-4px);
    box-shadow: 0 8px 24px rgba(0, 0, 0, 0.3);
  }
  
  .video-thumbnail {
    position: relative;
    height: 200px;
    background-color: var(--tertiary-bg);
  }
  
  .thumbnail-placeholder {
    width: 100%;
    height: 100%;
    display: flex;
    align-items: center;
    justify-content: center;
    background: linear-gradient(45deg, var(--secondary-bg) 25%, var(--tertiary-bg) 25%, var(--tertiary-bg) 50%, var(--secondary-bg) 50%, var(--secondary-bg) 75%, var(--tertiary-bg) 75%, var(--tertiary-bg) 100%);
    background-size: 40px 40px;
    opacity: 0.3;
  }
  
  .video-type-badge {
    position: absolute;
    top: 0.5rem;
    right: 0.5rem;
    background-color: rgba(0, 0, 0, 0.8);
    color: white;
    padding: 0.25rem 0.75rem;
    border-radius: 0.375rem;
    font-size: 0.75rem;
    text-transform: uppercase;
  }
  
  .video-overlay {
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background-color: rgba(0, 0, 0, 0.3);
    display: flex;
    align-items: center;
    justify-content: center;
    opacity: 0;
    transition: var(--transition);
  }
  
  .video-card:hover .video-overlay {
    opacity: 1;
  }
  
  .play-button {
    width: 48px;
    height: 48px;
    background-color: rgba(255, 255, 255, 0.9);
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 20px;
    transition: var(--transition);
  }
  
  .play-button:hover {
    background-color: white;
    transform: scale(1.1);
  }
  
  .video-info {
    padding: 1rem;
  }
  
  .video-title {
    font-size: 1.125rem;
    font-weight: 600;
    margin-bottom: 0.5rem;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }
  
  .video-meta {
    display: flex;
    justify-content: space-between;
    color: var(--secondary-text);
    font-size: 0.875rem;
    margin-bottom: 0.75rem;
  }
  
  .video-status {
    margin-bottom: 1rem;
    display: flex;
    align-items: center;
    gap: 0.5rem;
  }
  
  .status-badge {
    padding: 0.25rem 0.5rem;
    border-radius: 0.375rem;
    font-size: 0.75rem;
    text-transform: uppercase;
    font-weight: 500;
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
  
  .progress-text {
    font-size: 0.75rem;
    color: var(--secondary-text);
  }
  
  .btn-sm {
    padding: 0.5rem 1rem;
    font-size: 0.875rem;
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
  
  @media (max-width: 768px) {
    .header-content {
      flex-direction: column;
      align-items: flex-start;
      gap: 1rem;
    }
  }
  </style>