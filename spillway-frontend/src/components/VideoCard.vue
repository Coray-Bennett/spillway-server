<template>
  <div class="video-card" @click="navigateToVideo">
    <div class="video-thumbnail">
      <div class="thumbnail-overlay">
        <BaseIcon name="play" :size="48" class="play-icon" />
      </div>
      
      <!-- Status Badge -->
      <div 
        v-if="video.conversionStatus" 
        :class="['status-badge', video.conversionStatus.toLowerCase()]"
        title="Video Status"
      >
        {{ formatStatus(video.conversionStatus) }}
      </div>
      
      <!-- Encrypted Badge -->
      <div 
        v-if="video.encrypted"
        class="encrypted-badge"
        title="Encrypted Video"
        @click.stop="handleEncryptionClick"
      >
        <BaseIcon name="lock" :size="16" />
      </div>
    </div>
    
    <div class="video-info">
      <h3 class="video-title">{{ video.title }}</h3>
      
      <div class="video-meta">
        <div class="meta-item">
          <BaseIcon name="clock" :size="14" />
          <span>{{ formatDuration(video.length) }}</span>
        </div>
        
        <div v-if="video.type === 'EPISODE'" class="meta-item">
          <BaseIcon name="film" :size="14" />
          <span>S{{ video.seasonNumber || '?' }}:E{{ video.episodeNumber || '?' }}</span>
        </div>
        
        <div v-if="video.genre" class="meta-item">
          <BaseIcon name="tag" :size="14" />
          <span>{{ video.genre }}</span>
        </div>
      </div>
      
      <div class="video-details">
        <span class="upload-date">
          {{ formatDate(video.createdAt) }}
        </span>
        
        <div class="video-actions">
          <button 
            v-if="video.encrypted"
            @click.stop="showManageKeyModal"
            class="action-button"
            title="Manage Encryption Key"
          >
            <BaseIcon name="key" :size="16" />
          </button>
          <button 
            v-if="isOwner"
            @click.stop="navigateToEditVideo"
            class="action-button"
            title="Edit Video"
          >
            <BaseIcon name="edit" :size="16" />
          </button>
          <button 
            @click.stop="$emit('play', video)"
            class="action-button"
            title="Play Video"
          >
            <BaseIcon name="play" :size="16" />
          </button>
        </div>
      </div>
    </div>
    
    <!-- Key Management Modal -->
    <KeyManagementModal
      v-if="showKeyModal"
      :video-id="video.id"
      :video-title="video.title"
      :current-key="encryptionKey"
      @close="showKeyModal = false"
      @update="onKeyUpdate"
    />
    
    <!-- Encryption Key Modal -->
    <EncryptionKeyModal
      v-if="showKeyEntryModal"
      :video-id="video.id"
      :video-title="video.title"
      @close="showKeyEntryModal = false"
      @submit="onEncryptionKeySubmit"
    />
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { formatDate } from '@/utils/date'
import { formatDuration } from '@/utils/metadata'
import BaseIcon from '@/components/icons/BaseIcon.vue'
import KeyManagementModal from '@/components/KeyManagementModal.vue'
import EncryptionKeyModal from '@/components/EncryptionKeyModal.vue'
import encryptionKeyService from '@/services/encryptionKeyService'

const props = defineProps({
  video: {
    type: Object,
    required: true
  }
})

const emit = defineEmits(['play', 'keyUpdated'])

const router = useRouter()
const authStore = useAuthStore()
const showKeyModal = ref(false)
const showKeyEntryModal = ref(false)
const encryptionKey = ref(null)

const isOwner = computed(() => {
  return authStore.currentUsername === props.video.uploadedBy?.username
})

function formatStatus(status) {
  if (!status) return 'Unknown'
  const statusMap = {
    'PENDING': 'Pending',
    'IN_PROGRESS': 'Converting',
    'COMPLETED': 'Ready',
    'FAILED': 'Failed'
  }
  return statusMap[status] || status
}

function navigateToVideo() {
  router.push(`/video/${props.video.id}`)
}

function navigateToEditVideo() {
  // This would typically open an edit modal or navigate to an edit page
  router.push(`/video/${props.video.id}?edit=true`)
}

function showManageKeyModal() {
  // Check if we have a stored key
  const storedKey = encryptionKeyService.getKey(props.video.id)
  encryptionKey.value = storedKey
  showKeyModal.value = true
}

function handleEncryptionClick() {
  const hasKey = encryptionKeyService.hasKey(props.video.id)
  if (hasKey) {
    showManageKeyModal()
  } else {
    showKeyEntryModal.value = true
  }
}

function onEncryptionKeySubmit(key) {
  encryptionKey.value = key
  encryptionKeyService.storeKey(props.video.id, key)
  showKeyEntryModal.value = false
  emit('keyUpdated', { videoId: props.video.id, key })
}

function onKeyUpdate(newKey) {
  encryptionKey.value = newKey
  if (newKey) {
    encryptionKeyService.storeKey(props.video.id, newKey)
  } else {
    encryptionKeyService.removeKey(props.video.id)
  }
  showKeyModal.value = false
  emit('keyUpdated', { videoId: props.video.id, key: newKey })
}
</script>

<style scoped>
.video-card {
  background-color: var(--secondary-bg);
  border-radius: 0.75rem;
  overflow: hidden;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06);
  transition: transform 0.2s, box-shadow 0.2s;
  cursor: pointer;
  display: flex;
  flex-direction: column;
}

.video-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.1), 0 4px 6px -2px rgba(0, 0, 0, 0.05);
}

.video-thumbnail {
  position: relative;
  padding-top: 56.25%; /* 16:9 aspect ratio */
  background-color: #000;
  overflow: hidden;
}

.thumbnail-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(rgba(0, 0, 0, 0.1), rgba(0, 0, 0, 0.7));
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.2s;
}

.video-card:hover .thumbnail-overlay {
  opacity: 1;
}

.play-icon {
  color: white;
  filter: drop-shadow(0 2px 4px rgba(0, 0, 0, 0.3));
  transition: transform 0.2s;
}

.video-card:hover .play-icon {
  transform: scale(1.1);
}

.status-badge {
  position: absolute;
  top: 0.75rem;
  left: 0.75rem;
  padding: 0.25rem 0.5rem;
  border-radius: 9999px;
  font-size: 0.75rem;
  font-weight: 600;
  text-transform: lowercase;
  color: white;
}

.status-badge.pending {
  background-color: #94a3b8;
}

.status-badge.in_progress {
  background-color: var(--accent-color);
}

.status-badge.completed {
  background-color: var(--success-color);
}

.status-badge.failed {
  background-color: var(--danger-color);
}

.encrypted-badge {
  position: absolute;
  top: 0.75rem;
  right: 0.75rem;
  width: 2rem;
  height: 2rem;
  border-radius: 50%;
  background-color: rgba(0, 0, 0, 0.6);
  color: var(--accent-color);
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background-color 0.2s, transform 0.2s;
}

.encrypted-badge:hover {
  background-color: rgba(59, 130, 246, 0.8);
  color: white;
  transform: scale(1.1);
}

.video-info {
  padding: 1rem;
  flex: 1;
  display: flex;
  flex-direction: column;
}

.video-title {
  font-size: 1rem;
  font-weight: 600;
  margin: 0 0 0.75rem 0;
  color: var(--primary-text);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  line-height: 1.4;
}

.video-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
  margin-bottom: 0.75rem;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 0.25rem;
  color: var(--secondary-text);
  font-size: 0.75rem;
}

.video-details {
  margin-top: auto;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.upload-date {
  color: var(--secondary-text);
  font-size: 0.75rem;
}

.video-actions {
  display: flex;
  gap: 0.5rem;
}

.action-button {
  background: none;
  border: none;
  color: var(--secondary-text);
  padding: 0.375rem;
  border-radius: 0.375rem;
  cursor: pointer;
  transition: var(--transition);
  display: flex;
  align-items: center;
  justify-content: center;
}

.action-button:hover {
  background-color: var(--hover-bg);
  color: var(--primary-text);
}

.action-button:first-child {
  color: var(--accent-color);
}

@media (max-width: 640px) {
  .video-meta {
    flex-direction: column;
    gap: 0.375rem;
  }
}
</style>
