<template>
  <div 
    class="video-card" 
    :class="{ 'processing': isProcessing, 'failed': isFailed }"
    @click="$emit('select', video)"
  >
    <div class="video-thumbnail">
      <img v-if="video.thumbnailUrl" :src="video.thumbnailUrl" :alt="video.title">
      <div v-else class="placeholder-thumbnail">
        <BaseIcon name="video" :size="24" />
      </div>
      
      <!-- Status overlays -->
      <div v-if="isProcessing" class="processing-overlay">
        <LoadingSpinner :size="1.5" color="white" />
        <span>Processing</span>
      </div>
      
      <div v-else-if="isFailed" class="failed-overlay">
        <BaseIcon name="error" :size="28" />
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

<script setup>
import { computed } from 'vue'
import { useVideoStore } from '@/stores/video'
import { formatDate } from '@/utils/date'
import { formatDuration } from '@/utils/metadata'
import BaseIcon from './icons/BaseIcon.vue'
import LoadingSpinner from './common/LoadingSpinner.vue'

const props = defineProps({
  video: {
    type: Object,
    required: true
  }
})

defineEmits(['select'])

const videoStore = useVideoStore()

// Computed properties for video status
const isProcessing = computed(() => videoStore.isVideoProcessing(props.video))
const isFailed = computed(() => videoStore.isVideoFailed(props.video))
</script>

<style scoped>
.video-card {
  overflow: hidden;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
  transition: transform 0.2s, box-shadow 0.2s;
  background-color: var(--card-bg, #2a2a2a);
  cursor: pointer;
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
  background-color: var(--bg-secondary, #222);
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
  background-color: var(--bg-tertiary, #333);
  color: var(--text-muted, #999);
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

.processing-overlay,
.failed-overlay {
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

.video-info {
  padding: 12px;
}

.video-title {
  font-size: 0.95rem;
  margin: 0 0 6px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  color: var(--text-primary, #e0e0e0);
}

.video-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}

.video-genre {
  font-size: 0.75rem;
  color: var(--text-secondary, #b0b0b0);
  background-color: var(--bg-tertiary, #333);
  padding: 2px 6px;
  border-radius: 3px;
}

.video-uploader {
  font-size: 0.75rem;
  color: var(--text-secondary, #b0b0b0);
}

.video-stats {
  display: flex;
  justify-content: space-between;
  font-size: 0.75rem;
  color: var(--text-secondary, #b0b0b0);
}
</style>