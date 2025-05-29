<template>
  <div class="video-gallery">
    <div v-if="title" class="gallery-header">
      <h2 class="gallery-title">
        {{ title }}
        <span v-if="count" class="gallery-count">({{ count }})</span>
      </h2>
      
      <div class="gallery-actions" v-if="$slots.actions">
        <slot name="actions"></slot>
      </div>
    </div>
    
    <ErrorMessage v-if="error" :message="error" />
    
    <LoadingSpinner v-if="loading" />
    
    <div v-else-if="videos.length > 0" 
      :class="[
        'gallery-grid',
        `gallery-grid--${layout}`,
        { 'gallery-grid--compact': compact }
      ]"
    >
      <VideoCard
        v-for="video in videos"
        :key="video.id"
        :video="video"
        :show-actions="showVideoActions"
        :show-description="showVideoDescription"
        :show-uploader="showUploader"
        :compact="compact"
        @select="onSelect"
        @play="onPlay"
      >
        <template v-if="showVideoActions" #actions>
          <slot name="video-actions" :video="video"></slot>
        </template>
      </VideoCard>
    </div>
    
    <EmptyState
      v-else
      :title="emptyTitle"
      :description="emptyDescription"
      icon="video"
    >
      <slot name="empty-content"></slot>
      
      <template #actions>
        <slot name="empty-actions"></slot>
      </template>
    </EmptyState>
    
    <Pagination
      v-if="showPagination && totalPages > 1"
      :current-page="currentPage"
      :total-pages="totalPages"
      @page-change="onPageChange"
    />
  </div>
</template>

<script setup>
import { computed } from 'vue'
import VideoCard from './VideoCard.vue'
import ErrorMessage from './common/ErrorMessage.vue'
import LoadingSpinner from './common/LoadingSpinner.vue'
import EmptyState from './common/EmptyState.vue'
import Pagination from './common/Pagination.vue'

const props = defineProps({
  videos: {
    type: Array,
    default: () => []
  },
  layout: {
    type: String,
    default: 'grid',
    validator: (value) => ['grid', 'list', 'masonry'].includes(value)
  },
  title: {
    type: String,
    default: ''
  },
  loading: {
    type: Boolean,
    default: false
  },
  error: {
    type: String,
    default: ''
  },
  showVideoActions: {
    type: Boolean,
    default: false
  },
  showVideoDescription: {
    type: Boolean,
    default: false
  },
  showUploader: {
    type: Boolean,
    default: true
  },
  compact: {
    type: Boolean,
    default: false
  },
  currentPage: {
    type: Number,
    default: 0
  },
  totalPages: {
    type: Number,
    default: 1
  },
  emptyTitle: {
    type: String,
    default: 'No videos found'
  },
  emptyDescription: {
    type: String,
    default: 'There are no videos available to display.'
  },
  showPagination: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['select', 'play', 'page-change'])

// Calculate count for header display
const count = computed(() => props.videos.length)

// Event handlers
function onSelect(video) {
  emit('select', video)
}

function onPlay(video) {
  emit('play', video)
}

function onPageChange(page) {
  emit('page-change', page)
}
</script>

<style scoped>
.video-gallery {
  width: 100%;
}

.gallery-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
}

.gallery-title {
  font-size: 1.5rem;
  font-weight: 600;
  color: var(--text-primary, #e0e0e0);
  position: relative;
  padding-bottom: 0.5rem;
  margin: 0;
}

.gallery-title::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 0;
  width: 50px;
  height: 3px;
  background: var(--accent-primary, #3a86ff);
  border-radius: 2px;
}

.gallery-count {
  font-size: 1rem;
  color: var(--text-secondary, #b0b0b0);
  margin-left: 0.5rem;
  font-weight: 400;
}

.gallery-actions {
  display: flex;
  gap: 0.5rem;
}

/* Grid layouts */
.gallery-grid {
  display: grid;
  gap: 1.5rem;
  margin-bottom: 2rem;
}

.gallery-grid--grid {
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
}

.gallery-grid--grid.gallery-grid--compact {
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
}

.gallery-grid--list {
  grid-template-columns: 1fr;
  gap: 1rem;
}

.gallery-grid--masonry {
  columns: 3;
  column-gap: 1.5rem;
}

.gallery-grid--masonry > * {
  break-inside: avoid;
  margin-bottom: 1.5rem;
}

@media (max-width: 1200px) {
  .gallery-grid--masonry {
    columns: 3;
  }
}

@media (max-width: 900px) {
  .gallery-grid--masonry {
    columns: 2;
  }
}

@media (max-width: 640px) {
  .gallery-grid--masonry {
    columns: 1;
  }
  
  .gallery-grid--grid {
    grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  }
  
  .gallery-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 1rem;
  }
}
</style>