<template>
  <div 
    class="video-card" 
    :class="{ 
      'processing': isProcessing, 
      'failed': isFailed,
      'video-card--with-actions': showActions,
      'video-card--compact': compact
    }"
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
      
      <div v-else-if="video.duration" class="duration-badge">
        {{ formatDuration(video.duration) }}
      </div>
    </div>
    
    <div class="video-info">
      <h3 class="video-title" :title="video.title">
        {{ video.title }}
      </h3>
      
      <div class="video-meta">
        <div class="video-meta-item" v-if="video.genre">
          <BaseIcon name="tag" :size="14" />
          <span class="video-genre">{{ video.genre }}</span>
        </div>
        
        <div class="video-meta-item" v-if="showUploader && video.uploadedBy">
          <BaseIcon name="user" :size="14" />
          <span class="video-uploader">{{ video.uploadedBy.username || 'Unknown' }}</span>
        </div>
        
        <div class="video-meta-item" v-if="video.createdAt && !isProcessing && !isFailed">
          <BaseIcon name="calendar" :size="14" />
          <span class="video-date">{{ formatDate(video.createdAt) }}</span>
        </div>
        
        <div class="video-meta-item" v-if="video.views !== undefined">
          <BaseIcon name="eye" :size="14" />
          <span class="video-views">{{ formatViewCount(video.views) }}</span>
        </div>
      </div>
      
      <p v-if="showDescription && video.description" class="video-description">
        {{ truncateDescription(video.description) }}
      </p>
      
      <div v-if="showActions && !compact" class="video-actions">
        <slot name="actions"></slot>
      </div>
    </div>
    
    <div v-if="showActions && compact" class="compact-actions">
      <slot name="actions"></slot>
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
import AppButton from './common/AppButton.vue'

const props = defineProps({
  video: {
    type: Object,
    required: true
  },
  showActions: {
    type: Boolean,
    default: false
  },
  showDescription: {
    type: Boolean,
    default: false
  },
  showUploader: {
    type: Boolean,
    default: true
  },
  descriptionLength: {
    type: Number,
    default: 120
  },
  compact: {
    type: Boolean,
    default: false
  }
})

defineEmits(['select', 'play'])

const videoStore = useVideoStore()

// Computed properties for video status
const isProcessing = computed(() => videoStore.isVideoProcessing(props.video))
const isFailed = computed(() => videoStore.isVideoFailed(props.video))

// Helper methods
function truncateDescription(text) {
  if (!text) return ''
  return text.length > props.descriptionLength 
    ? text.slice(0, props.descriptionLength) + '...' 
    : text
}

function formatViewCount(views) {
  if (views === undefined || views === null) return '0 views'
  
  if (views < 1000) return `${views} view${views === 1 ? '' : 's'}`
  if (views < 1000000) return `${(views / 1000).toFixed(1)}K views`
  return `${(views / 1000000).toFixed(1)}M views`
}
</script>

<style scoped>
.video-card {
  overflow: hidden;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
  transition: transform 0.2s, box-shadow 0.2s;
  background-color: var(--card-bg, #2a2a2a);
  cursor: pointer;
  display: flex;
  flex-direction: column;
}

.video-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 5px 15px rgba(0,0,0,0.1);
}

.video-card--compact {
  flex-direction: row;
  align-items: center;
}

.video-card--compact .video-thumbnail {
  width: 180px;
  height: 100px;
  flex-shrink: 0;
}

.video-card--compact .video-info {
  flex-grow: 1;
  display: flex;
  flex-direction: column;
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

.video-card--compact .video-thumbnail {
  padding-top: 0;
}

.video-thumbnail img {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.video-card--compact .video-thumbnail img {
  position: relative;
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

.video-card--compact .placeholder-thumbnail {
  position: relative;
  height: 100px;
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

.thumbnail-actions {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: rgba(0, 0, 0, 0.5);
  opacity: 0;
  transition: opacity 0.2s;
}

.video-card:hover .thumbnail-actions {
  opacity: 1;
}

.play-btn {
  transform: scale(0.9);
  transition: transform 0.2s;
}

.video-card:hover .play-btn {
  transform: scale(1);
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
  display: flex;
  flex-direction: column;
  flex-grow: 1;
}

.video-title {
  font-size: 0.95rem;
  margin: 0 0 8px;
  display: -webkit-box;
  line-clamp: 2;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-overflow: ellipsis;
  color: var(--text-primary, #e0e0e0);
  line-height: 1.3;
}

.video-card--compact .video-title {
  line-clamp: 1;
  -webkit-line-clamp: 1;
  margin-bottom: 6px;
}

.video-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 8px;
  font-size: 0.75rem;
  color: var(--text-secondary, #b0b0b0);
}

.video-meta-item {
  display: flex;
  align-items: center;
  gap: 4px;
}

.video-description {
  font-size: 0.85rem;
  color: var(--text-secondary, #b0b0b0);
  line-height: 1.4;
  margin: 0 0 12px;
}

.video-actions,
.compact-actions {
  display: flex;
  gap: 8px;
  margin-top: auto;
}

.video-card--with-actions:hover {
  background-color: var(--card-hover-bg, #323232);
}

@media (max-width: 640px) {
  .video-card--compact {
    flex-direction: column;
  }
  
  .video-card--compact .video-thumbnail {
    width: 100%;
    padding-top: 56.25%;
  }
  
  .video-card--compact .video-thumbnail img {
    position: absolute;
  }
  
  .video-card--compact .placeholder-thumbnail {
    position: absolute;
    height: 100%;
  }
}
</style>