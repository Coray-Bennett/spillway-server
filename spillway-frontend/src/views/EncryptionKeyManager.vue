<template>
  <div class="encryption-manager">
    <div class="encryption-manager-header">
      <h2 class="encryption-manager-title">
        <BaseIcon name="key" :size="24" />
        Encryption Keys Manager
      </h2>
      <div class="encryption-stats">
        <span v-if="keyStats.totalKeys > 0">{{ keyStats.totalKeys }} keys stored</span>
      </div>
    </div>

    <div class="encryption-manager-tabs">
      <button
        v-for="tab in tabs"
        :key="tab.id"
        :class="['tab-button', { active: activeTab === tab.id }]"
        @click="activeTab = tab.id"
      >
        <BaseIcon :name="tab.icon" :size="16" />
        {{ tab.label }}
      </button>
    </div>

    <div class="encryption-manager-content">
      <!-- Keys list tab -->
      <div v-if="activeTab === 'keys'" class="tab-content">
        <div class="actions-bar">
          <div class="search-container">
            <input
              v-model="searchQuery"
              type="text"
              class="search-input"
              placeholder="Search keys..."
            />
            <BaseIcon name="search" :size="16" class="search-icon" />
          </div>
          <button @click="refreshKeys" class="refresh-btn">
            <BaseIcon name="refresh" :size="16" />
            Refresh
          </button>
        </div>

        <div v-if="filteredKeys.length === 0" class="empty-state">
          <BaseIcon name="key-off" :size="48" class="empty-icon" />
          <p v-if="searchQuery">No keys match your search.</p>
          <p v-else>No encryption keys stored yet.</p>
        </div>

        <div v-else class="keys-list">
          <div
            v-for="(keyData, videoId) in filteredKeys"
            :key="videoId"
            class="key-item"
          >
            <div class="key-info">
              <div class="key-meta">
                <h3 class="key-title">
                  <BaseIcon name="video" :size="16" />
                  {{ getVideoTitle(videoId) || 'Video ' + truncateId(videoId) }}
                </h3>
                <span class="key-date">
                  <BaseIcon name="calendar" :size="14" />
                  {{ formatDate(keyData.createdAt) }}
                </span>
              </div>
              <div class="key-actions">
                <button
                  @click="() => viewKey(videoId, keyData.key)"
                  class="action-btn"
                  title="View Key"
                >
                  <BaseIcon name="eye" :size="16" />
                </button>
                <button
                  @click="() => copyKey(keyData.key)"
                  class="action-btn"
                  title="Copy Key"
                >
                  <BaseIcon name="copy" :size="16" />
                </button>
                <button
                  @click="() => editKey(videoId)"
                  class="action-btn edit"
                  title="Edit Key"
                >
                  <BaseIcon name="edit" :size="16" />
                </button>
                <button
                  @click="() => deleteKey(videoId)"
                  class="action-btn delete"
                  title="Delete Key"
                >
                  <BaseIcon name="trash" :size="16" />
                </button>
              </div>
            </div>
            <div v-if="videoId === selectedKeyId" class="key-detail">
              <div class="key-display">
                <span>{{ maskedKey }}</span>
                <button @click="toggleKeyVisibility" class="toggle-visibility-btn">
                  <BaseIcon :name="showKey ? 'eye-off' : 'eye'" :size="16" />
                </button>
              </div>
              <button @click="selectedKeyId = null" class="close-detail-btn">
                <BaseIcon name="chevron-up" :size="16" />
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- Export/Import tab -->
      <div v-if="activeTab === 'export'" class="tab-content">
        <div class="export-section">
          <div class="section-card">
            <h3 class="section-title">
              <BaseIcon name="download" :size="20" />
              Export Keys
            </h3>
            <p class="section-desc">Export all your encryption keys as a backup file. Keep this file secure.</p>
            
            <div class="stats-card" v-if="keyStats.totalKeys > 0">
              <div class="stat-item">
                <span class="stat-label">Total Keys:</span>
                <span class="stat-value">{{ keyStats.totalKeys }}</span>
              </div>
              <div class="stat-item" v-if="keyStats.oldestKey">
                <span class="stat-label">Oldest Key:</span>
                <span class="stat-value">{{ formatDate(keyStats.oldestKey) }}</span>
              </div>
              <div class="stat-item" v-if="keyStats.newestKey">
                <span class="stat-label">Newest Key:</span>
                <span class="stat-value">{{ formatDate(keyStats.newestKey) }}</span>
              </div>
            </div>
            
            <div class="actions">
              <button 
                @click="exportKeys" 
                class="btn btn-primary"
                :disabled="keyStats.totalKeys === 0"
              >
                <BaseIcon name="download" :size="16" />
                Export All Keys
              </button>
            </div>
          </div>

          <div class="section-card">
            <h3 class="section-title">
              <BaseIcon name="upload" :size="20" />
              Import Keys
            </h3>
            <p class="section-desc">Import previously exported encryption keys.</p>
            
            <div class="form-group">
              <label class="checkbox-label">
                <input type="checkbox" v-model="mergeOnImport" class="checkbox-input" />
                <span>Merge with existing keys</span>
              </label>
            </div>
            
            <div class="file-upload-area">
              <input
                ref="importFileInput"
                type="file"
                accept=".json"
                @change="handleImportFile"
                class="file-input"
              />
              <div class="file-upload-zone" @click="$refs.importFileInput.click()">
                <div v-if="!selectedImportFile" class="upload-content">
                  <BaseIcon name="upload" :size="32" class="upload-icon" />
                  <p class="upload-text">Click to select keys file</p>
                  <p class="upload-hint">JSON format (.json)</p>
                </div>
                <div v-else class="selected-file">
                  <BaseIcon name="file" :size="20" class="file-icon" />
                  <div class="file-info">
                    <p class="file-name">{{ selectedImportFile.name }}</p>
                    <p class="file-size">{{ formatFileSize(selectedImportFile.size) }}</p>
                  </div>
                  <button @click.stop="selectedImportFile = null" class="remove-file-btn">
                    <BaseIcon name="close" :size="16" />
                  </button>
                </div>
              </div>
            </div>
            
            <div class="actions">
              <button 
                @click="importKeys" 
                class="btn btn-primary"
                :disabled="!selectedImportFile"
              >
                <BaseIcon name="upload" :size="16" />
                Import Keys
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- Settings tab -->
      <div v-if="activeTab === 'settings'" class="tab-content">
        <div class="section-card">
          <h3 class="section-title">
            <BaseIcon name="settings" :size="20" />
            Key Storage Settings
          </h3>

          <div class="settings-group">
            <div class="form-group">
              <label class="form-label">Storage Method</label>
              <div class="radio-group">
                <label class="radio-label">
                  <input type="radio" v-model="storageMethod" value="local" class="radio-input" />
                  <span>Browser Local Storage (Default)</span>
                </label>
                <label class="radio-label disabled">
                  <input type="radio" v-model="storageMethod" value="encrypted" class="radio-input" disabled />
                  <span>Encrypted Cloud Storage (Coming soon)</span>
                </label>
              </div>
            </div>

            <div class="form-group">
              <button @click="confirmClearStorage" class="btn btn-danger">
                <BaseIcon name="trash" :size="16" />
                Clear All Stored Keys
              </button>
            </div>
          </div>
        </div>

        <div class="section-card">
          <h3 class="section-title">
            <BaseIcon name="info" :size="20" />
            About Encryption Keys
          </h3>
          
          <div class="info-content">
            <p>
              Encryption keys are stored locally in your browser and are used to protect your video content.
              Without the correct key, encrypted videos cannot be played.
            </p>
            <p class="warning">
              <BaseIcon name="alert" :size="16" />
              Never share your encryption keys unless you want to grant access to your encrypted videos.
            </p>
            <p>
              We recommend regularly exporting your keys as a backup in case you need to access your videos
              from a different device or browser.
            </p>
          </div>
        </div>
      </div>
    </div>

    <!-- Key edit modal -->
    <div v-if="showEditModal" class="modal-overlay" @click.self="showEditModal = false">
      <div class="modal-container">
        <div class="modal-header">
          <h3 class="modal-title">Edit Encryption Key</h3>
          <button @click="showEditModal = false" class="close-btn">
            <BaseIcon name="close" :size="20" />
          </button>
        </div>

        <div class="modal-body">
          <div class="form-group">
            <label class="form-label">Video ID</label>
            <input type="text" :value="editingKeyId" class="form-input" readonly />
          </div>

          <div class="form-group">
            <label class="form-label">New Encryption Key</label>
            <div class="input-group">
              <input
                v-model="newKeyValue"
                :type="showEditKey ? 'text' : 'password'"
                class="form-input"
                placeholder="Enter new encryption key"
                @keyup.enter="updateKey"
              />
              <button 
                @click="showEditKey = !showEditKey" 
                class="icon-btn"
              >
                <BaseIcon :name="showEditKey ? 'eye-off' : 'eye'" :size="16" />
              </button>
            </div>
          </div>
        </div>

        <div class="modal-footer">
          <button @click="showEditModal = false" class="btn btn-secondary">Cancel</button>
          <button 
            @click="updateKey" 
            class="btn btn-primary"
            :disabled="!newKeyValue.trim()"
          >
            <BaseIcon name="save" :size="16" />
            Save Changes
          </button>
        </div>
      </div>
    </div>

    <!-- Notifications -->
    <div v-if="notification" :class="['notification', notification.type]">
      <BaseIcon :name="notification.icon" :size="16" />
      <span>{{ notification.message }}</span>
      <button @click="notification = null" class="close-notification">
        <BaseIcon name="close" :size="14" />
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, reactive, onMounted } from 'vue'
import BaseIcon from '../components/icons/BaseIcon.vue'
import encryptionKeyService from '@/services/encryptionKeyService'
import { useVideoStore } from '@/stores/video'

const videoStore = useVideoStore()

// State
const activeTab = ref('keys')
const searchQuery = ref('')
const keys = ref({})
const videoTitles = ref({})
const keyStats = ref({
  totalKeys: 0,
  oldestKey: null,
  newestKey: null,
  lastUsed: null
})
const selectedKeyId = ref(null)
const showKey = ref(false)
const currentKeyValue = ref('')
const notification = ref(null)
const mergeOnImport = ref(true)
const selectedImportFile = ref(null)
const storageMethod = ref('local')

// Editing state
const showEditModal = ref(false)
const editingKeyId = ref(null)
const newKeyValue = ref('')
const showEditKey = ref(false)

// Tabs definition
const tabs = [
  { id: 'keys', label: 'My Keys', icon: 'key' },
  { id: 'export', label: 'Export/Import', icon: 'download' },
  { id: 'settings', label: 'Settings', icon: 'settings' }
]

// Computed
const filteredKeys = computed(() => {
  const query = searchQuery.value.toLowerCase().trim()
  if (!query) return keys.value

  const filtered = {}
  Object.entries(keys.value).forEach(([videoId, keyData]) => {
    const videoTitle = getVideoTitle(videoId) || ''
    if (
      videoId.toLowerCase().includes(query) || 
      videoTitle.toLowerCase().includes(query)
    ) {
      filtered[videoId] = keyData
    }
  })
  return filtered
})

const maskedKey = computed(() => {
  if (!showKey.value && currentKeyValue.value) {
    return '•'.repeat(Math.min(currentKeyValue.value.length, 30))
  }
  return currentKeyValue.value
})

// Methods
function loadKeys() {
  keys.value = encryptionKeyService.getAllKeys()
  keyStats.value = encryptionKeyService.getStats()
  
  // Try to get video titles for all keys
  fetchVideoTitles()
}

async function fetchVideoTitles() {
  const videoIds = Object.keys(keys.value)
  
  // Batch fetch video titles from the API or use cached data
  for (const videoId of videoIds) {
    if (!videoTitles.value[videoId]) {
      try {
        const video = await videoStore.getVideo(videoId)
        if (video && video.title) {
          videoTitles.value[videoId] = video.title
        }
      } catch (error) {
        console.error(`Failed to fetch title for video ${videoId}:`, error)
      }
    }
  }
}

function getVideoTitle(videoId) {
  return videoTitles.value[videoId] || null
}

function truncateId(id) {
  if (!id) return ''
  if (id.length <= 8) return id
  return id.substring(0, 4) + '...' + id.substring(id.length - 4)
}

function formatDate(dateStr) {
  if (!dateStr) return 'N/A'
  const date = new Date(dateStr)
  return date.toLocaleString()
}

function formatFileSize(bytes) {
  if (bytes === 0) return '0 Bytes'
  const k = 1024
  const sizes = ['Bytes', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

function refreshKeys() {
  loadKeys()
  showNotification('Keys refreshed', 'success', 'refresh')
}

function viewKey(videoId, key) {
  selectedKeyId.value = selectedKeyId.value === videoId ? null : videoId
  currentKeyValue.value = key
  showKey.value = false
}

function toggleKeyVisibility() {
  showKey.value = !showKey.value
}

async function copyKey(key) {
  try {
    await navigator.clipboard.writeText(key)
    showNotification('Key copied to clipboard', 'success', 'check')
  } catch (err) {
    console.error('Failed to copy key:', err)
    showNotification('Failed to copy key', 'error', 'error')
  }
}

function editKey(videoId) {
  editingKeyId.value = videoId
  newKeyValue.value = keys.value[videoId]?.key || ''
  showEditModal.value = true
}

function updateKey() {
  if (!newKeyValue.value.trim()) {
    showNotification('Key cannot be empty', 'error', 'error')
    return
  }

  encryptionKeyService.storeKey(editingKeyId.value, newKeyValue.value)
  loadKeys()
  showEditModal.value = false
  showNotification('Key updated successfully', 'success', 'check')
}

function deleteKey(videoId) {
  if (confirm(`Are you sure you want to delete the encryption key for this video? You won't be able to access the encrypted content without it.`)) {
    encryptionKeyService.removeKey(videoId)
    loadKeys()
    if (selectedKeyId.value === videoId) {
      selectedKeyId.value = null
    }
    showNotification('Key deleted', 'success', 'trash')
  }
}

function exportKeys() {
  try {
    const keysJson = encryptionKeyService.exportKeys()
    const blob = new Blob([keysJson], { type: 'application/json' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `spillway-encryption-keys-${new Date().toISOString().split('T')[0]}.json`
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    URL.revokeObjectURL(url)
    
    showNotification('Keys exported successfully', 'success', 'download')
  } catch (err) {
    console.error('Failed to export keys:', err)
    showNotification('Failed to export keys', 'error', 'error')
  }
}

function handleImportFile(event) {
  const file = event.target.files[0]
  if (file) {
    selectedImportFile.value = file
  }
}

function importKeys() {
  if (!selectedImportFile.value) return

  const reader = new FileReader()
  reader.onload = (e) => {
    try {
      const keysJson = e.target.result
      encryptionKeyService.importKeys(keysJson, mergeOnImport.value)
      loadKeys()
      selectedImportFile.value = null
      showNotification('Keys imported successfully', 'success', 'check')
    } catch (err) {
      console.error('Failed to import keys:', err)
      showNotification('Failed to import keys: Invalid file format', 'error', 'error')
    }
  }
  reader.onerror = () => {
    showNotification('Failed to read file', 'error', 'error')
  }
  reader.readAsText(selectedImportFile.value)
}

function confirmClearStorage() {
  if (confirm('Are you sure you want to clear all stored encryption keys? This action cannot be undone.')) {
    encryptionKeyService.clearAllKeys()
    loadKeys()
    showNotification('All encryption keys cleared', 'success', 'trash')
  }
}

function showNotification(message, type = 'info', icon = 'info') {
  notification.value = { message, type, icon }
  setTimeout(() => {
    if (notification.value && notification.value.message === message) {
      notification.value = null
    }
  }, 5000)
}

// Lifecycle
onMounted(() => {
  loadKeys()
})
</script>

<style scoped>
.encryption-manager {
  width: 100%;
  max-width: 900px;
  margin: 0 auto;
  padding: 2rem 1rem;
  min-height: calc(100vh - 4rem);
  position: relative;
}

.encryption-manager-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.5rem;
}

.encryption-manager-title {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  font-size: 1.75rem;
  font-weight: 700;
  color: var(--primary-text);
  margin: 0;
}

.encryption-stats {
  color: var(--secondary-text);
  font-size: 0.9375rem;
  display: flex;
  align-items: center;
}

.encryption-manager-tabs {
  display: flex;
  border-bottom: 1px solid var(--border-color);
  margin-bottom: 1.5rem;
  gap: 0.5rem;
}

.tab-button {
  background: none;
  border: none;
  color: var(--secondary-text);
  font-size: 0.9375rem;
  font-weight: 500;
  padding: 1rem;
  cursor: pointer;
  transition: var(--transition);
  border-bottom: 2px solid transparent;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.tab-button.active {
  color: var(--accent-color);
  border-bottom-color: var(--accent-color);
}

.tab-button:hover:not(.active) {
  color: var(--primary-text);
  background-color: var(--tertiary-bg);
}

.encryption-manager-content {
  position: relative;
  min-height: 300px;
}

.actions-bar {
  display: flex;
  align-items: center;
  gap: 1rem;
  margin-bottom: 1.5rem;
}

.search-container {
  position: relative;
  flex: 1;
  max-width: 400px;
}

.search-input {
  width: 100%;
  padding: 0.75rem;
  padding-left: 2.5rem;
  border: 1px solid var(--border-color);
  border-radius: 0.5rem;
  background-color: var(--tertiary-bg);
  color: var(--primary-text);
  transition: var(--transition);
}

.search-input:focus {
  outline: none;
  border-color: var(--accent-color);
  background-color: var(--secondary-bg);
}

.search-icon {
  position: absolute;
  top: 50%;
  left: 0.75rem;
  transform: translateY(-50%);
  color: var(--secondary-text);
  pointer-events: none;
}

.refresh-btn {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.75rem 1rem;
  border: none;
  background-color: var(--tertiary-bg);
  color: var(--secondary-text);
  border-radius: 0.5rem;
  cursor: pointer;
  transition: var(--transition);
}

.refresh-btn:hover {
  background-color: var(--hover-bg);
  color: var(--primary-text);
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 3rem 1rem;
  color: var(--secondary-text);
  text-align: center;
}

.empty-icon {
  margin-bottom: 1rem;
  color: var(--tertiary-bg);
}

.keys-list {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.key-item {
  background-color: var(--secondary-bg);
  border: 1px solid var(--border-color);
  border-radius: 0.75rem;
  overflow: hidden;
  transition: var(--transition);
}

.key-item:hover {
  border-color: var(--accent-color);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.key-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1rem;
}

.key-meta {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.key-title {
  margin: 0;
  font-size: 1rem;
  font-weight: 600;
  color: var(--primary-text);
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.key-date {
  display: flex;
  align-items: center;
  gap: 0.25rem;
  color: var(--secondary-text);
  font-size: 0.8125rem;
}

.key-actions {
  display: flex;
  gap: 0.5rem;
}

.action-btn {
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

.action-btn:hover {
  background-color: var(--hover-bg);
  color: var(--primary-text);
}

.action-btn.edit:hover {
  color: var(--accent-color);
}

.action-btn.delete:hover {
  color: var(--danger-color);
}

.key-detail {
  padding: 1rem;
  background-color: var(--tertiary-bg);
  border-top: 1px solid var(--border-color);
  position: relative;
}

.key-display {
  background-color: var(--primary-bg);
  padding: 0.75rem;
  border-radius: 0.375rem;
  color: var(--primary-text);
  font-family: monospace;
  font-size: 0.875rem;
  word-break: break-all;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 1rem;
}

.toggle-visibility-btn {
  background: none;
  border: none;
  color: var(--secondary-text);
  flex-shrink: 0;
  cursor: pointer;
  transition: var(--transition);
}

.toggle-visibility-btn:hover {
  color: var(--accent-color);
}

.close-detail-btn {
  position: absolute;
  top: -0.625rem;
  right: 1rem;
  background-color: var(--tertiary-bg);
  border: 1px solid var(--border-color);
  border-radius: 9999px;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 1.5rem;
  height: 1.5rem;
  padding: 0;
  cursor: pointer;
  transition: var(--transition);
}

.close-detail-btn:hover {
  background-color: var(--hover-bg);
}

/* Export/Import Styles */
.export-section {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 1.5rem;
}

.section-card {
  background-color: var(--secondary-bg);
  border: 1px solid var(--border-color);
  border-radius: 0.75rem;
  padding: 1.5rem;
  margin-bottom: 1.5rem;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 1.125rem;
  font-weight: 600;
  color: var(--primary-text);
  margin: 0 0 0.75rem 0;
}

.section-desc {
  color: var(--secondary-text);
  font-size: 0.875rem;
  margin: 0 0 1.5rem 0;
}

.stats-card {
  background-color: var(--tertiary-bg);
  border-radius: 0.5rem;
  padding: 1rem;
  margin-bottom: 1.5rem;
}

.stat-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.5rem;
}

.stat-item:last-child {
  margin-bottom: 0;
}

.stat-label {
  color: var(--secondary-text);
  font-size: 0.875rem;
}

.stat-value {
  color: var(--primary-text);
  font-weight: 500;
  font-size: 0.875rem;
}

.form-group {
  margin-bottom: 1.5rem;
}

.form-label {
  display: block;
  font-size: 0.875rem;
  font-weight: 500;
  color: var(--primary-text);
  margin-bottom: 0.5rem;
}

.file-upload-area {
  position: relative;
  margin-bottom: 1.5rem;
}

.file-input {
  display: none;
}

.file-upload-zone {
  border: 2px dashed var(--border-color);
  border-radius: 0.75rem;
  padding: 2rem 1rem;
  text-align: center;
  cursor: pointer;
  transition: var(--transition);
  display: flex;
  align-items: center;
  justify-content: center;
}

.file-upload-zone:hover {
  border-color: var(--accent-color);
  background-color: rgba(59, 130, 246, 0.05);
}

.upload-content {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.upload-icon {
  color: var(--accent-color);
  margin-bottom: 0.75rem;
}

.upload-text {
  font-weight: 500;
  color: var(--primary-text);
  margin: 0 0 0.25rem 0;
}

.upload-hint {
  color: var(--secondary-text);
  font-size: 0.75rem;
  margin: 0;
}

.selected-file {
  display: flex;
  align-items: center;
  width: 100%;
  gap: 0.75rem;
  padding: 0.75rem;
  background-color: var(--tertiary-bg);
  border-radius: 0.5rem;
}

.file-info {
  flex: 1;
  text-align: left;
}

.file-name {
  font-weight: 500;
  color: var(--primary-text);
  margin: 0 0 0.25rem 0;
}

.file-size {
  color: var(--secondary-text);
  font-size: 0.75rem;
  margin: 0;
}

.remove-file-btn {
  background: none;
  border: none;
  color: var(--secondary-text);
  cursor: pointer;
  transition: var(--transition);
  padding: 0.25rem;
  display: flex;
  align-items: center;
  justify-content: center;
}

.remove-file-btn:hover {
  color: var(--danger-color);
}

/* Settings Styles */
.settings-group {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.radio-group {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.radio-label {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  color: var(--primary-text);
  cursor: pointer;
  font-size: 0.9375rem;
}

.radio-label.disabled {
  color: var(--secondary-text);
  cursor: not-allowed;
}

.info-content {
  color: var(--secondary-text);
  font-size: 0.9375rem;
  line-height: 1.6;
}

.info-content p {
  margin: 0 0 1rem 0;
}

.info-content p:last-child {
  margin-bottom: 0;
}

.warning {
  color: var(--warning-color);
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.75rem;
  background-color: rgba(250, 204, 21, 0.1);
  border-radius: 0.375rem;
}

/* Modal Styles */
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

.modal-container {
  background-color: var(--secondary-bg);
  border-radius: 0.75rem;
  max-width: 450px;
  width: 100%;
  box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 10px 10px -5px rgba(0, 0, 0, 0.04);
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 1rem 1.5rem;
  border-bottom: 1px solid var(--border-color);
}

.modal-title {
  margin: 0;
  font-size: 1.125rem;
  font-weight: 600;
}

.close-btn {
  background: none;
  border: none;
  color: var(--secondary-text);
  cursor: pointer;
  padding: 0.5rem;
  border-radius: 0.375rem;
  display: flex;
  align-items: center;
  justify-content: center;
}

.close-btn:hover {
  background-color: var(--hover-bg);
  color: var(--primary-text);
}

.modal-body {
  padding: 1.5rem;
}

.form-input {
  width: 100%;
  padding: 0.75rem;
  border: 1px solid var(--border-color);
  border-radius: 0.5rem;
  background-color: var(--primary-bg);
  color: var(--primary-text);
  transition: var(--transition);
}

.form-input:focus {
  outline: none;
  border-color: var(--accent-color);
}

.form-input:read-only {
  background-color: var(--tertiary-bg);
}

.input-group {
  display: flex;
  gap: 0.5rem;
}

.icon-btn {
  background-color: var(--primary-bg);
  border: 1px solid var(--border-color);
  border-radius: 0.5rem;
  padding: 0.75rem;
  color: var(--secondary-text);
  cursor: pointer;
  transition: var(--transition);
  display: flex;
  align-items: center;
  justify-content: center;
}

.icon-btn:hover {
  background-color: var(--hover-bg);
  color: var(--primary-text);
}

.checkbox-label {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  color: var(--primary-text);
  cursor: pointer;
  font-size: 0.9375rem;
}

.checkbox-input {
  width: 1.25rem;
  height: 1.25rem;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 1rem;
  padding: 1.25rem 1.5rem;
  border-top: 1px solid var(--border-color);
}

/* Button Styles */
.actions {
  display: flex;
  gap: 1rem;
}

.btn {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.75rem 1.25rem;
  border-radius: 0.5rem;
  font-weight: 500;
  font-size: 0.9375rem;
  border: none;
  cursor: pointer;
  transition: var(--transition);
  text-decoration: none;
}

.btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-primary {
  background-color: var(--accent-color);
  color: white;
}

.btn-primary:hover:not(:disabled) {
  background-color: var(--accent-hover);
}

.btn-secondary {
  background-color: var(--tertiary-bg);
  color: var(--primary-text);
}

.btn-secondary:hover:not(:disabled) {
  background-color: var(--hover-bg);
}

.btn-danger {
  background-color: var(--danger-color);
  color: white;
}

.btn-danger:hover:not(:disabled) {
  opacity: 0.9;
}

/* Notification Styles */
.notification {
  position: fixed;
  bottom: 1.5rem;
  right: 1.5rem;
  padding: 1rem 1.5rem;
  border-radius: 0.5rem;
  display: flex;
  align-items: center;
  gap: 0.75rem;
  max-width: 350px;
  box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.1), 0 4px 6px -2px rgba(0, 0, 0, 0.05);
  z-index: 1100;
  animation: slideIn 0.3s ease;
}

.notification.success {
  background-color: rgba(16, 185, 129, 0.1);
  border: 1px solid rgba(16, 185, 129, 0.3);
  color: var(--success-color);
}

.notification.error {
  background-color: rgba(239, 68, 68, 0.1);
  border: 1px solid rgba(239, 68, 68, 0.3);
  color: var(--danger-color);
}

.notification.info {
  background-color: rgba(59, 130, 246, 0.1);
  border: 1px solid rgba(59, 130, 246, 0.3);
  color: var(--accent-color);
}

.close-notification {
  background: none;
  border: none;
  color: inherit;
  opacity: 0.7;
  cursor: pointer;
  padding: 0.25rem;
  margin-left: auto;
  display: flex;
  align-items: center;
  justify-content: center;
}

.close-notification:hover {
  opacity: 1;
}

@keyframes slideIn {
  from {
    transform: translateY(100%);
    opacity: 0;
  }
  to {
    transform: translateY(0);
    opacity: 1;
  }
}

/* Responsive Styles */
@media (max-width: 768px) {
  .encryption-manager {
    padding: 1.5rem 0.5rem;
  }

  .encryption-manager-title {
    font-size: 1.5rem;
  }

  .encryption-manager-tabs {
    overflow-x: auto;
    padding-bottom: 2px;
  }

  .tab-button {
    white-space: nowrap;
    padding: 0.75rem;
  }

  .actions-bar {
    flex-direction: column;
    align-items: stretch;
  }

  .search-container {
    max-width: 100%;
  }

  .export-section {
    grid-template-columns: 1fr;
  }

  .modal-footer {
    flex-direction: column-reverse;
  }

  .modal-footer .btn {
    width: 100%;
  }
}
</style>