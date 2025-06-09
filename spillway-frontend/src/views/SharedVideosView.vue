<template>
  <div class="shared-videos-view">
    <div class="page-header">
      <h1>Shared Videos</h1>
      <p class="page-subtitle">Videos that have been shared with you and videos you've shared</p>
    </div>

    <!-- Tab Navigation -->
    <div class="tab-navigation">
      <button 
        @click="activeTab = 'sharedWithMe'"
        :class="['tab-btn', { active: activeTab === 'sharedWithMe' }]"
      >
        <BaseIcon name="inbox" :size="16" />
        Shared with Me ({{ sharedWithMe.length }})
      </button>
      <button 
        @click="activeTab = 'myShares'"
        :class="['tab-btn', { active: activeTab === 'myShares' }]"
      >
        <BaseIcon name="share" :size="16" />
        My Shares ({{ myCreatedShares.length }})
      </button>
    </div>

    <!-- Loading State -->
    <div v-if="sharingStore.isLoading" class="loading-state">
      <div class="spinner"></div>
      <p>Loading shared videos...</p>
    </div>

    <!-- Error State -->
    <div v-if="sharingStore.error" class="error-state">
      <BaseIcon name="alert-circle" :size="48" />
      <h3>Error Loading Shares</h3>
      <p>{{ sharingStore.error }}</p>
      <button @click="loadData" class="btn btn-primary">Try Again</button>
    </div>

    <!-- Shared with Me Tab -->
    <div v-if="activeTab === 'sharedWithMe' && !sharingStore.isLoading" class="tab-content">
      <div v-if="sharedWithMe.length === 0" class="empty-state">
        <BaseIcon name="video-off" :size="48" />
        <h3>No Shared Videos</h3>
        <p>No one has shared any videos with you yet.</p>
      </div>

      <div v-else class="shares-grid">
        <div
          v-for="share in sharedWithMe"
          :key="share.id"
          class="share-card"
        >
          <div class="share-header">
            <h3 class="video-title">{{ share.videoTitle }}</h3>
            <span class="permission-badge" :class="`permission-${share.permission.toLowerCase()}`">
              {{ share.permission }}
            </span>
          </div>
          
          <div class="share-info">
            <div class="share-detail">
              <BaseIcon name="user" :size="16" />
              <span>Shared by: <strong>{{ share.sharedByUsername }}</strong></span>
            </div>
            <div class="share-detail">
              <BaseIcon name="calendar" :size="16" />
              <span>Shared: {{ formatDate(share.createdAt) }}</span>
            </div>
            <div v-if="share.expiresAt" class="share-detail expires">
              <BaseIcon name="clock" :size="16" />
              <span>Expires: {{ formatDate(share.expiresAt) }}</span>
            </div>
          </div>
          
          <div class="share-status">
            <span :class="['status-badge', { active: share.isValid }]">
              {{ share.isValid ? 'Active' : 'Expired' }}
            </span>
          </div>
          
          <div class="share-actions">
            <router-link 
              :to="`/video/${share.videoId}`"
              class="btn btn-primary"
              :disabled="!share.isValid"
            >
              <BaseIcon name="play" :size="16" />
              Watch Video
            </router-link>
          </div>
        </div>
      </div>
    </div>

    <!-- My Shares Tab -->
    <div v-if="activeTab === 'myShares' && !sharingStore.isLoading" class="tab-content">
      <div v-if="myCreatedShares.length === 0" class="empty-state">
        <BaseIcon name="share" :size="48" />
        <h3>No Shares Created</h3>
        <p>You haven't shared any videos yet.</p>
        <router-link to="/videos" class="btn btn-primary">
          Go to My Videos
        </router-link>
      </div>

      <div v-else class="shares-grid">
        <div
          v-for="share in myCreatedShares"
          :key="share.id"
          class="share-card"
        >
          <div class="share-header">
            <h3 class="video-title">{{ share.videoTitle }}</h3>
            <span class="permission-badge" :class="`permission-${share.permission.toLowerCase()}`">
              {{ share.permission }}
            </span>
          </div>
          
          <div class="share-info">
            <div class="share-detail">
              <BaseIcon name="user" :size="16" />
              <span>Shared with: <strong>{{ share.sharedWithUsername }}</strong></span>
            </div>
            <div class="share-detail">
              <BaseIcon name="calendar" :size="16" />
              <span>Shared: {{ formatDate(share.createdAt) }}</span>
            </div>
            <div v-if="share.expiresAt" class="share-detail expires">
              <BaseIcon name="clock" :size="16" />
              <span>Expires: {{ formatDate(share.expiresAt) }}</span>
            </div>
          </div>
          
          <div class="share-status">
            <span :class="['status-badge', { active: share.isValid }]">
              {{ share.isValid ? 'Active' : 'Expired' }}
            </span>
          </div>
          
          <div class="share-actions">
            <button
              @click="revokeShare(share.id)"
              class="btn btn-danger"
              :disabled="!share.isValid"
            >
              <BaseIcon name="x" :size="16" />
              Revoke Access
            </button>
            <router-link 
              :to="`/video/${share.videoId}`"
              class="btn btn-outline"
            >
              <BaseIcon name="eye" :size="16" />
              View Video
            </router-link>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useVideoSharingStore } from '@/stores/videoSharing'
import BaseIcon from '@/components/icons/BaseIcon.vue'
import { formatDate } from '@/utils/date'

const sharingStore = useVideoSharingStore()
const activeTab = ref('sharedWithMe')

const sharedWithMe = computed(() => sharingStore.sharedWithMe)
const myCreatedShares = computed(() => sharingStore.myCreatedShares)

onMounted(() => {
  loadData()
})

async function loadData() {
  sharingStore.clearError()
  
  if (activeTab.value === 'sharedWithMe') {
    await sharingStore.getSharedWithMe()
  } else {
    await sharingStore.getMyCreatedShares()
  }
}

async function revokeShare(shareId) {
  const result = await sharingStore.revokeShare(shareId)
  
  if (result.success) {
    // Refresh the current tab data
    await loadData()
  }
}

// Watch for tab changes to load appropriate data
watch(activeTab, (newTab) => {
  if (newTab === 'sharedWithMe') {
    sharingStore.getSharedWithMe()
  } else {
    sharingStore.getMyCreatedShares()
  }
})
</script>

<style scoped>
.shared-videos-view {
  max-width: 1200px;
  margin: 0 auto;
  padding: 2rem;
}

.page-header {
  margin-bottom: 2rem;
}

.page-header h1 {
  margin: 0 0 0.5rem 0;
  color: var(--primary-text);
}

.page-subtitle {
  color: var(--secondary-text);
  margin: 0;
}

.tab-navigation {
  display: flex;
  gap: 0.5rem;
  margin-bottom: 2rem;
  border-bottom: 1px solid var(--border-color);
}

.tab-btn {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 1rem 1.5rem;
  border: none;
  background: none;
  color: var(--secondary-text);
  cursor: pointer;
  border-bottom: 2px solid transparent;
  transition: var(--transition);
  font-weight: 500;
}

.tab-btn:hover {
  color: var(--primary-text);
  background-color: var(--hover-bg);
}

.tab-btn.active {
  color: var(--accent-color);
  border-bottom-color: var(--accent-color);
}

.loading-state, .error-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 1rem;
  padding: 4rem 2rem;
  text-align: center;
}

.spinner {
  width: 48px;
  height: 48px;
  border: 4px solid var(--border-color);
  border-top: 4px solid var(--accent-color);
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.error-state {
  color: var(--danger-color);
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 1rem;
  padding: 4rem 2rem;
  text-align: center;
  color: var(--secondary-text);
}

.shares-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(400px, 1fr));
  gap: 1.5rem;
}

.share-card {
  background-color: var(--secondary-bg);
  border: 1px solid var(--border-color);
  border-radius: 0.75rem;
  padding: 1.5rem;
  transition: var(--transition);
}

.share-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);
}

.share-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 1rem;
  gap: 1rem;
}

.video-title {
  margin: 0;
  color: var(--primary-text);
  font-size: 1.125rem;
  line-height: 1.4;
}

.permission-badge {
  padding: 0.25rem 0.75rem;
  border-radius: 0.5rem;
  font-size: 0.75rem;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  white-space: nowrap;
}

.permission-read {
  background-color: rgba(34, 197, 94, 0.1);
  color: #22c55e;
}

.permission-modify {
  background-color: rgba(59, 130, 246, 0.1);
  color: #3b82f6;
}

.permission-admin {
  background-color: rgba(147, 51, 234, 0.1);
  color: #9333ea;
}

.share-info {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  margin-bottom: 1rem;
}

.share-detail {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 0.875rem;
  color: var(--secondary-text);
}

.share-detail.expires {
  color: var(--warning-color);
}

.share-status {
  margin-bottom: 1rem;
}

.status-badge {
  padding: 0.25rem 0.75rem;
  border-radius: 0.375rem;
  font-size: 0.75rem;
  font-weight: 600;
  text-transform: uppercase;
  background-color: rgba(239, 68, 68, 0.1);
  color: #ef4444;
}

.status-badge.active {
  background-color: rgba(34, 197, 94, 0.1);
  color: #22c55e;
}

.share-actions {
  display: flex;
  gap: 0.75rem;
  flex-wrap: wrap;
}

.share-actions .btn {
  flex: 1;
  min-width: 120px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
}

@media (max-width: 768px) {
  .shared-videos-view {
    padding: 1rem;
  }
  
  .shares-grid {
    grid-template-columns: 1fr;
  }
  
  .tab-navigation {
    flex-direction: column;
  }
  
  .share-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 0.5rem;
  }
  
  .share-actions {
    flex-direction: column;
  }
  
  .share-actions .btn {
    min-width: auto;
  }
}
</style>