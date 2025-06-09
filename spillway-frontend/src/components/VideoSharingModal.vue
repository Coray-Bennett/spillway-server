<template>
  <div class="modal-overlay" @click.self="$emit('close')">
    <div class="modal-content">
      <div class="modal-header">
        <h3>Share Video: {{ video.title }}</h3>
        <button @click="$emit('close')" class="close-btn">
          <BaseIcon name="x" :size="20" />
        </button>
      </div>

      <div class="modal-body">
        <!-- Share Form -->
        <form @submit.prevent="shareVideo" class="share-form" v-if="!showShares">
          <div class="form-group">
            <label for="username">Share with Username:</label>
            <input
              id="username"
              v-model="shareForm.sharedWithUsername"
              type="text"
              class="form-input"
              placeholder="Enter username"
              required
            />
          </div>

          <div class="form-group">
            <label for="permission">Permission Level:</label>
            <select id="permission" v-model="shareForm.permission" class="form-select">
              <option value="READ">Read Only (can view)</option>
              <option value="MODIFY">Modify (can edit metadata)</option>
              <option value="ADMIN">Admin (can share with others)</option>
            </select>
          </div>

          <div class="form-group">
            <label for="expires">Expires At (optional):</label>
            <input
              id="expires"
              v-model="shareForm.expiresAt"
              type="datetime-local"
              class="form-input"
            />
          </div>

          <div class="form-actions">
            <button type="button" @click="$emit('close')" class="btn btn-secondary">
              Cancel
            </button>
            <button type="submit" :disabled="sharingStore.isLoading" class="btn btn-primary">
              <span v-if="sharingStore.isLoading">Sharing...</span>
              <span v-else>Share Video</span>
            </button>
          </div>
        </form>

        <!-- Current Shares -->
        <div v-if="!showShares" class="shares-section">
          <div class="section-header">
            <h4>Current Shares</h4>
            <button @click="toggleSharesView" class="btn btn-outline">
              View All Shares
            </button>
          </div>

          <div v-if="currentShares.length === 0" class="empty-state">
            <p>This video hasn't been shared yet.</p>
          </div>

          <div v-else class="shares-list">
            <div
              v-for="share in currentShares.slice(0, 3)"
              :key="share.id"
              class="share-item"
            >
              <div class="share-info">
                <span class="username">{{ share.sharedWithUsername }}</span>
                <span class="permission">{{ share.permission }}</span>
                <span v-if="share.expiresAt" class="expires">
                  Expires: {{ formatDate(share.expiresAt) }}
                </span>
              </div>
              <button
                @click="revokeShare(share.id)"
                class="btn btn-danger btn-sm"
              >
                Revoke
              </button>
            </div>
          </div>
        </div>

        <!-- All Shares View -->
        <div v-if="showShares" class="all-shares">
          <div class="section-header">
            <h4>All Shares for This Video</h4>
            <button @click="toggleSharesView" class="btn btn-outline">
              Back to Share
            </button>
          </div>

          <div v-if="currentShares.length === 0" class="empty-state">
            <p>This video hasn't been shared yet.</p>
          </div>

          <div v-else class="shares-list">
            <div
              v-for="share in currentShares"
              :key="share.id"
              class="share-item detailed"
            >
              <div class="share-info">
                <div class="share-main">
                  <span class="username">{{ share.sharedWithUsername }}</span>
                  <span class="permission badge">{{ share.permission }}</span>
                </div>
                <div class="share-meta">
                  <span class="created">Shared: {{ formatDate(share.createdAt) }}</span>
                  <span v-if="share.expiresAt" class="expires">
                    Expires: {{ formatDate(share.expiresAt) }}
                  </span>
                  <span class="status" :class="{ active: share.isValid }">
                    {{ share.isValid ? 'Active' : 'Expired' }}
                  </span>
                </div>
              </div>
              <button
                @click="revokeShare(share.id)"
                class="btn btn-danger btn-sm"
                :disabled="!share.isValid"
              >
                Revoke
              </button>
            </div>
          </div>
        </div>

        <!-- Error Message -->
        <div v-if="sharingStore.error" class="error-message">
          {{ sharingStore.error }}
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useVideoSharingStore } from '@/stores/videoSharing'
import BaseIcon from './icons/BaseIcon.vue'
import { formatDate as formatDateUtil } from '@/utils/date'

const props = defineProps({
  video: {
    type: Object,
    required: true
  }
})

const emit = defineEmits(['close', 'shared'])

const sharingStore = useVideoSharingStore()
const showShares = ref(false)

const shareForm = reactive({
  sharedWithUsername: '',
  permission: 'READ',
  expiresAt: ''
})

const currentShares = computed(() => {
  return sharingStore.getSharesForVideoId(props.video.id)
})

onMounted(() => {
  // Load existing shares for this video
  sharingStore.getSharesForVideo(props.video.id)
})

async function shareVideo() {
  const shareRequest = {
    videoId: props.video.id,
    sharedWithUsername: shareForm.sharedWithUsername,
    permission: shareForm.permission,
    expiresAt: shareForm.expiresAt || null
  }

  const result = await sharingStore.shareVideo(shareRequest)
  
  if (result.success) {
    // Reset form
    shareForm.sharedWithUsername = ''
    shareForm.permission = 'READ'
    shareForm.expiresAt = ''
    
    // Refresh shares for this video
    await sharingStore.getSharesForVideo(props.video.id)
    
    emit('shared', result.share)
  }
}

async function revokeShare(shareId) {
  const result = await sharingStore.revokeShare(shareId)
  
  if (result.success) {
    // Refresh shares after revocation
    await sharingStore.getSharesForVideo(props.video.id)
  }
}

function toggleSharesView() {
  showShares.value = !showShares.value
}

function formatDate(dateString) {
  return formatDateUtil(dateString)
}
</script>

<style scoped>
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.7);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  padding: 1rem;
}

.modal-content {
  background-color: var(--secondary-bg);
  border-radius: 0.75rem;
  max-width: 600px;
  width: 100%;
  max-height: 80vh;
  overflow-y: auto;
  border: 1px solid var(--border-color);
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1.5rem;
  border-bottom: 1px solid var(--border-color);
}

.modal-header h3 {
  margin: 0;
  color: var(--primary-text);
}

.close-btn {
  background: none;
  border: none;
  color: var(--secondary-text);
  cursor: pointer;
  padding: 0.5rem;
  border-radius: 0.375rem;
  transition: var(--transition);
}

.close-btn:hover {
  background-color: var(--hover-bg);
}

.modal-body {
  padding: 1.5rem;
}

.share-form {
  margin-bottom: 2rem;
}

.form-group {
  margin-bottom: 1rem;
}

.form-group label {
  display: block;
  margin-bottom: 0.5rem;
  font-weight: 500;
  color: var(--primary-text);
}

.form-input, .form-select {
  width: 100%;
  padding: 0.75rem;
  border: 1px solid var(--border-color);
  border-radius: 0.5rem;
  background-color: var(--primary-bg);
  color: var(--primary-text);
  transition: var(--transition);
}

.form-input:focus, .form-select:focus {
  outline: none;
  border-color: var(--accent-color);
  box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.1);
}

.form-actions {
  display: flex;
  gap: 1rem;
  justify-content: flex-end;
}

.shares-section, .all-shares {
  border-top: 1px solid var(--border-color);
  padding-top: 1.5rem;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
}

.section-header h4 {
  margin: 0;
  color: var(--primary-text);
}

.shares-list {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.share-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1rem;
  background-color: var(--primary-bg);
  border-radius: 0.5rem;
  border: 1px solid var(--border-color);
}

.share-item.detailed {
  align-items: flex-start;
}

.share-info {
  flex: 1;
}

.share-main {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-bottom: 0.5rem;
}

.username {
  font-weight: 600;
  color: var(--primary-text);
}

.permission {
  font-size: 0.875rem;
  color: var(--secondary-text);
}

.badge {
  padding: 0.25rem 0.5rem;
  background-color: var(--accent-color);
  color: white;
  border-radius: 0.25rem;
  font-size: 0.75rem;
  font-weight: 500;
}

.share-meta {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
  font-size: 0.875rem;
  color: var(--secondary-text);
}

.status.active {
  color: var(--success-color);
}

.expires {
  color: var(--warning-color);
}

.empty-state {
  text-align: center;
  padding: 2rem;
  color: var(--secondary-text);
}

.error-message {
  background-color: rgba(239, 68, 68, 0.1);
  border: 1px solid rgba(239, 68, 68, 0.3);
  color: #ef4444;
  padding: 0.75rem;
  border-radius: 0.5rem;
  margin-top: 1rem;
}

@media (max-width: 768px) {
  .modal-overlay {
    padding: 0.5rem;
  }
  
  .form-actions {
    flex-direction: column;
  }
  
  .share-item {
    flex-direction: column;
    align-items: flex-start;
    gap: 0.75rem;
  }
  
  .section-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 0.5rem;
  }
}
</style>