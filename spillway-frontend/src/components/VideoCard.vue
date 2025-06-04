<template>
  <div class="video-card" :class="{ 'processing': isProcessing, 'failed': isFailed }">
    <div class="video-thumbnail">
      <img v-if="video.thumbnailUrl" :src="video.thumbnailUrl" :alt="video.title">
      <div v-else class="placeholder-thumbnail">
        <i class="fas fa-video"></i>
      </div>
      
      <div v-if="isProcessing" class="processing-overlay">
        <div class="spinner"></div>
        <span>Processing</span>
      </div>
      
      <div v-else-if="isFailed" class="failed-overlay">
        <i class="fas fa-exclamation-circle"></i>
        <span>Processing Failed</span>
      </div>
      
      <div v-else class="duration-badge">
        {{ formatDuration(video.duration) }}
      </div>
    </div>
    
    <div class="video-info">
      <h3 class="video-title" :title="video.title">{{ video.title }}</h3>
      
      <div class="video-meta">
        <span v-if="video.genre" class="video-genre">{{ video.genre }}</span>
        <span class="video-uploader">{{ video.uploadedBy?.username || 'Unknown' }}</span>
      </div>
      
      <div class="video-stats" v-if="!isProcessing && !isFailed">
        <span v-if="video.createdAt" class="video-date">
          {{ formatDate(video.createdAt) }}
        </span>
      </div>
    </div>
  </div>
</template>

<script>
import { useVideoStore } from '../stores/video'
import { formatDate } from '@/utils/date'
import { formatDuration } from '@/utils/metadata'

export default {
  name: 'VideoCard',
  
  props: {
    video: {
      type: Object,
      required: true
    }
  },
  
  setup() {
    const videoStore = useVideoStore()
    return { videoStore }
  },
  
  computed: {
    isProcessing() {
      return this.videoStore.isVideoProcessing(this.video)
    },
    
    isFailed() {
      return this.videoStore.isVideoFailed(this.video)
    }
  },
  
  methods: {
    formatDuration,
    formatDate
  }
}
</script>

<style scoped>
.video-card {
  overflow: hidden;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
  transition: transform 0.2s, box-shadow 0.2s;
  background-color: white;
}

.video-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 5px 15px rgba(0,0,0,0.1);
}

.video-card.processing {
  opacity: 0.8;
}

.video-card.failed {
  opacity: 0.7;
}

.video-thumbnail {
  position: relative;
  padding-top: 56.25%; /* 16:9 Aspect Ratio */
  background-color: #f1f1f1;
}

.video-thumbnail img {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.placeholder-thumbnail {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #eaeaea;
  color: #aaa;
  font-size: 2rem;
}

.duration-badge {
  position: absolute;
  bottom: 8px;
  right: 8px;
  background-color: rgba(0,0,0,0.7);
  color: white;
  padding: 2px 6px;
  border-radius: 3px;
  font-size: 0.75rem;
  font-weight: bold;
}

.processing-overlay, .failed-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background-color: rgba(0,0,0,0.5);
  color: white;
}

.failed-overlay {
  background-color: rgba(200,0,0,0.5);
}

.spinner {
  width: 24px;
  height: 24px;
  border: 3px solid rgba(255,255,255,0.3);
  border-radius: 50%;
  border-top-color: white;
  animation: spin 1s ease-in-out infinite;
  margin-bottom: 8px;
}

.fa-exclamation-circle {
  font-size: 28px;
  margin-bottom: 8px;
}

.video-info {
  padding: 12px;
}

.video-title {
  font-size: 0.95rem;
  margin: 0 0 6px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  color: --primary-text;
}

.video-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}

.video-genre {
  font-size: 0.75rem;
  color: --secondary-text;
  background-color: --secondary-bg;
  padding: 2px 6px;
  border-radius: 3px;
}

.video-uploader {
  font-size: 0.75rem;
  color: --secondary-text;
}

.video-stats {
  display: flex;
  justify-content: space-between;
  font-size: 0.75rem;
  color: --secondary-text;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}
</style>