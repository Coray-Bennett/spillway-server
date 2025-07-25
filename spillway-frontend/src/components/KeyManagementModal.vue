<template>
  <div class="modal-overlay" @click.self="$emit('close')">
    <div class="modal-container">
      <div class="modal-header">
        <h2 class="modal-title">
          <BaseIcon name="key" :size="24" />
          Manage Encryption Key
        </h2>
        <button @click="$emit('close')" class="close-btn">
          <BaseIcon name="close" :size="20" />
        </button>
      </div>

      <div class="modal-body">
        <div v-if="videoTitle" class="video-info">
          <p class="video-title-display">{{ videoTitle }}</p>
        </div>

        <div class="tabs">
          <button 
            :class="['tab-btn', { active: activeTab === 'current' }]"
            @click="activeTab = 'current'"
          >
            Current Key
          </button>
          <button 
            :class="['tab-btn', { active: activeTab === 'change' }]"
            @click="activeTab = 'change'"
          >
            Change Key
          </button>
          <button 
            :class="['tab-btn', { active: activeTab === 'export' }]"
            @click="activeTab = 'export'"
          >
            Export/Import
          </button>
        </div>

        <div class="tab-content">
          <!-- Current Key Tab -->
          <div v-if="activeTab === 'current'" class="current-key-section">
            <div v-if="hasStoredKey" class="key-status">
              <BaseIcon name="check-circle" :size="20" class="success-icon" />
              <p>Encryption key is stored locally for this video.</p>
            </div>
            <div v-else class="key-status">
              <BaseIcon name="alert-circle" :size="20" class="warning-icon" />
              <p>No encryption key found for this video.</p>
            </div>

            <div v-if="currentKey" class="form-group">
              <label class="form-label">Current Encryption Key</label>
              <div class="input-group">
                <input
                  :type="showCurrentKey ? 'text' : 'password'"
                  :value="currentKey"
                  class="form-input key-input"
                  readonly
                />
                <button 
                  @click="showCurrentKey = !showCurrentKey"
                  class="icon-btn"
                  title="Toggle visibility"
                >
                  <BaseIcon :name="showCurrentKey ? 'eye-off' : 'eye'" :size="16" />
                </button>
                <button 
                  @click="copyCurrentKey"
                  class="icon-btn"
                  title="Copy key"
                >
                  <BaseIcon name="copy" :size="16" />
                </button>
              </div>

              <div v-if="keyInfo" class="key-metadata">
                <p><strong>Created:</strong> {{ formatDate(keyInfo.createdAt) }}</p>
                <p><strong>Last Used:</strong> {{ formatDate(keyInfo.lastUsed) }}</p>
              </div>
            </div>

            <button 
              v-if="hasStoredKey"
              @click="removeStoredKey"
              class="btn btn-danger"
            >
              <BaseIcon name="trash" :size="16" />
              Remove Stored Key
            </button>
          </div>

          <!-- Change Key Tab -->
          <div v-if="activeTab === 'change'" class="change-key-section">
            <div class="info-message">
              <BaseIcon name="info" :size="16" />
              <p>Enter a new encryption key to replace the current one. This will only update your local storage.</p>
            </div>

            <div class="form-group">
              <label class="form-label">New Encryption Key</label>
              <div class="input-group">
                <input
                  v-model="newKey"
                  :type="showNewKey ? 'text' : 'password'"
                  class="form-input"
                  placeholder="Enter new encryption key"
                  @keyup.enter="updateKey"
                />
                <button 
                  @click="showNewKey = !showNewKey"
                  class="icon-btn"
                  title="Toggle visibility"
                >
                  <BaseIcon :name="showNewKey ? 'eye-off' : 'eye'" :size="16" />
                </button>
              </div>
            </div>

            <button 
              @click="updateKey"
              class="btn btn-primary"
              :disabled="!newKey.trim()"
            >
              <BaseIcon name="save" :size="16" />
              Update Key
            </button>
          </div>

          <!-- Export/Import Tab -->
          <div v-if="activeTab === 'export'" class="export-section">
            <div class="export-group">
              <h3 class="section-title">Export Keys</h3>
              <p class="section-desc">Export all your stored encryption keys for backup.</p>
              
              <div class="stats" v-if="keyStats.totalKeys > 0">
                <p><strong>Total Keys:</strong> {{ keyStats.totalKeys }}</p>
                <p v-if="keyStats.oldestKey"><strong>Oldest:</strong> {{ formatDate(keyStats.oldestKey) }}</p>
                <p v-if="keyStats.newestKey"><strong>Newest:</strong> {{ formatDate(keyStats.newestKey) }}</p>
              </div>

              <button @click="exportKeys" class="btn btn-secondary">
                <BaseIcon name="download" :size="16" />
                Export All Keys
              </button>
            </div>

            <div class="import-group">
              <h3 class="section-title">Import Keys</h3>
              <p class="section-desc">Import previously exported encryption keys.</p>
              
              <div class="form-group">
                <input
                  ref="importFileInput"
                  type="file"
                  accept=".json"
                  @change="handleImportFile"
                  style="display: none"
                />
                <button 
                  @click="$refs.importFileInput.click()"
                  class="btn btn-secondary"
                >
                  <BaseIcon name="upload" :size="16" />
                  Choose File
                </button>
              </div>

              <div class="form-group">
                <label class="checkbox-label">
                  <input
                    type="checkbox"
                    v-model="mergeOnImport"
                    class="checkbox-input"
                  />
                  <span>Merge with existing keys</span>
                </label>
              </div>
            </div>
          </div>
        </div>

        <div v-if="error" class="error-message">
          <BaseIcon name="error" :size="16" />
          {{ error }}
        </div>

        <div v-if="successMessage" class="success-message">
          <BaseIcon name="check" :size="16" />
          {{ successMessage }}
        </div>
      </div>

      <div class="modal-footer">
        <button @click="$emit('close')" class="btn btn-secondary">
          Close
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import BaseIcon from './icons/BaseIcon.vue'
import encryptionKeyService from '@/services/encryptionKeyService'

const props = defineProps({
  videoId: {
    type: String,
    required: true
  },
  videoTitle: {
    type: String,
    default: ''
  },
  currentKey: {
    type: String,
    default: null
  }
})

const emit = defineEmits(['close', 'update'])

const activeTab = ref('current')
const showCurrentKey = ref(false)
const showNewKey = ref(false)
const newKey = ref('')
const error = ref('')
const successMessage = ref('')
const mergeOnImport = ref(true)
const keyInfo = ref(null)
const keyStats = ref({
  totalKeys: 0,
  oldestKey: null,
  newestKey: null,
  lastUsed: null
})

const hasStoredKey = computed(() => {
  return encryptionKeyService.hasKey(props.videoId)
})

onMounted(() => {
  loadKeyInfo()
  loadKeyStats()
})

function loadKeyInfo() {
  const keys = encryptionKeyService.getAllKeys()
  if (keys[props.videoId]) {
    keyInfo.value = keys[props.videoId]
  }
}

function loadKeyStats() {
  keyStats.value = encryptionKeyService.getStats()
}

function formatDate(dateStr) {
  if (!dateStr) return 'N/A'
  const date = new Date(dateStr)
  return date.toLocaleString()
}

async function copyCurrentKey() {
  if (!props.currentKey) return

  try {
    await navigator.clipboard.writeText(props.currentKey)
    showSuccess('Encryption key copied to clipboard!')
  } catch (err) {
    console.error('Failed to copy key:', err)
    showError('Failed to copy encryption key')
  }
}

function updateKey() {
  const key = newKey.value.trim()
  
  if (!key) {
    showError('Please enter an encryption key')
    return
  }

  emit('update', key)
  showSuccess('Encryption key updated successfully!')
  newKey.value = ''
  activeTab.value = 'current'
  loadKeyInfo()
}

function removeStoredKey() {
  if (confirm('Are you sure you want to remove the stored encryption key? You will need to enter it manually next time.')) {
    encryptionKeyService.removeKey(props.videoId)
    emit('update', null)
    showSuccess('Encryption key removed')
    loadKeyInfo()
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
    
    showSuccess('Keys exported successfully!')
  } catch (err) {
    console.error('Failed to export keys:', err)
    showError('Failed to export keys')
  }
}

function handleImportFile(event) {
  const file = event.target.files[0]
  if (!file) return

  const reader = new FileReader()
  reader.onload = (e) => {
    try {
      const keysJson = e.target.result
      encryptionKeyService.importKeys(keysJson, mergeOnImport.value)
      showSuccess('Keys imported successfully!')
      loadKeyInfo()
      loadKeyStats()
      
      // Reset file input
      event.target.value = ''
    } catch (err) {
      console.error('Failed to import keys:', err)
      showError('Failed to import keys: Invalid file format')
    }
  }
  reader.onerror = () => {
    showError('Failed to read file')
  }
  reader.readAsText(file)
}

function showError(message) {
  error.value = message
  successMessage.value = ''
  setTimeout(() => {
    error.value = ''
  }, 5000)
}

function showSuccess(message) {
  successMessage.value = message
  error.value = ''
  setTimeout(() => {
    successMessage.value = ''
  }, 5000)
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

.modal-container {
  background: var(--secondary-bg);
  border-radius: 1rem;
  max-width: 600px;
  width: 100%;
  max-height: 90vh;
  overflow-y: auto;
  box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1), 0 10px 10px -5px rgba(0, 0, 0, 0.04);
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 1.5rem;
  border-bottom: 1px solid var(--border-color);
}

.modal-title {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 1.25rem;
  font-weight: 600;
  color: var(--primary-text);
  margin: 0;
}

.close-btn {
  background: none;
  border: none;
  color: var(--secondary-text);
  cursor: pointer;
  padding: 0.5rem;
  border-radius: 0.5rem;
  transition: var(--transition);
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

.video-info {
  margin-bottom: 1.5rem;
}

.video-title-display {
  font-weight: 500;
  color: var(--primary-text);
  margin: 0;
}

.tabs {
  display: flex;
  gap: 0.5rem;
  margin-bottom: 1.5rem;
  border-bottom: 1px solid var(--border-color);
}

.tab-btn {
  background: none;
  border: none;
  padding: 0.75rem 1rem;
  color: var(--secondary-text);
  font-size: 0.9375rem;
  font-weight: 500;
  cursor: pointer;
  transition: var(--transition);
  border-bottom: 2px solid transparent;
}

.tab-btn.active {
  color: var(--accent-color);
  border-bottom-color: var(--accent-color);
}

.tab-btn:hover {
  color: var(--primary-text);
}

.tab-content {
  min-height: 200px;
}

.key-status {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 1rem;
  background-color: var(--tertiary-bg);
  border-radius: 0.5rem;
  margin-bottom: 1.5rem;
}

.success-icon {
  color: var(--success-color);
}

.warning-icon {
  color: var(--warning-color);
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

.input-group {
  display: flex;
  gap: 0.5rem;
}

.form-input {
  flex: 1;
  padding: 0.75rem 1rem;
  background-color: var(--primary-bg);
  border: 1px solid var(--border-color);
  border-radius: 0.5rem;
  color: var(--primary-text);
  font-size: 1rem;
  transition: var(--transition);
}

.key-input {
  font-family: monospace;
  font-size: 0.875rem;
}

.form-input:focus {
  outline: none;
  border-color: var(--accent-color);
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

.form-input::placeholder {
  color: var(--secondary-text);
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

.key-metadata {
  margin-top: 0.75rem;
  padding: 0.75rem;
  background-color: var(--tertiary-bg);
  border-radius: 0.375rem;
  font-size: 0.875rem;
  color: var(--secondary-text);
}

.key-metadata p {
  margin: 0.25rem 0;
}

.info-message {
  display: flex;
  align-items: flex-start;
  gap: 0.5rem;
  padding: 0.75rem 1rem;
  background-color: rgba(59, 130, 246, 0.1);
  border: 1px solid rgba(59, 130, 246, 0.3);
  border-radius: 0.5rem;
  margin-bottom: 1.5rem;
  color: var(--accent-color);
  font-size: 0.875rem;
}

.info-message p {
  margin: 0;
}

.export-section {
  display: flex;
  flex-direction: column;
  gap: 2rem;
}

.export-group,
.import-group {
  padding: 1.25rem;
  background-color: var(--tertiary-bg);
  border-radius: 0.5rem;
}

.section-title {
  font-size: 1rem;
  font-weight: 600;
  color: var(--primary-text);
  margin: 0 0 0.5rem 0;
}

.section-desc {
  color: var(--secondary-text);
  font-size: 0.875rem;
  margin: 0 0 1rem 0;
}

.stats {
  background-color: var(--primary-bg);
  padding: 0.75rem;
  border-radius: 0.375rem;
  margin-bottom: 1rem;
  font-size: 0.875rem;
}

.stats p {
  margin: 0.25rem 0;
  color: var(--secondary-text);
}

.checkbox-label {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  cursor: pointer;
  font-size: 0.9375rem;
  color: var(--primary-text);
}

.checkbox-input {
  width: 1.25rem;
  height: 1.25rem;
  cursor: pointer;
}

.error-message,
.success-message {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.75rem 1rem;
  border-radius: 0.5rem;
  margin-top: 1rem;
  font-size: 0.875rem;
}

.error-message {
  background-color: rgba(239, 68, 68, 0.1);
  border: 1px solid rgba(239, 68, 68, 0.3);
  color: var(--danger-color);
}

.success-message {
  background-color: rgba(16, 185, 129, 0.1);
  border: 1px solid rgba(16, 185, 129, 0.3);
  color: var(--success-color);
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 1rem;
  padding: 1.5rem;
  border-top: 1px solid var(--border-color);
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

.btn-secondary:hover {
  background-color: var(--hover-bg);
}

.btn-danger {
  background-color: var(--danger-color);
  color: white;
}

.btn-danger:hover {
  opacity: 0.9;
}

@media (max-width: 640px) {
  .modal-container {
    max-height: 100vh;
    border-radius: 0;
  }

  .tabs {
    font-size: 0.875rem;
  }

  .tab-btn {
    padding: 0.5rem 0.75rem;
  }
}
</style>