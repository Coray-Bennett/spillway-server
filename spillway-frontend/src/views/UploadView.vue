<template>
    <div class="upload-view">
      <div class="container">
        <header class="page-header">
          <h1 class="page-title">Upload Video</h1>
        </header>
        
        <div class="upload-container">
          <div class="upload-card card">
            <div class="upload-tabs">
              <button
                :class="['tab-button', { active: activeTab === 'video' }]"
                @click="activeTab = 'video'"
              >
                Single Video
              </button>
              <button
                :class="['tab-button', { active: activeTab === 'playlist' }]"
                @click="activeTab = 'playlist'"
              >
              <BaseIcon name="plus" :size="16" />
                Create Playlist
              </button>
            </div>
            
            <div v-if="activeTab === 'video'" class="tab-content">
              <form @submit.prevent="handleVideoUpload" class="upload-form">
                <div class="form-grid">
                  <div class="form-group">
                    <label for="title" class="form-label">Title</label>
                    <input
                      id="title"
                      v-model="videoForm.title"
                      type="text"
                      class="form-input"
                      required
                    />
                  </div>
                  
                  <div class="form-group">
                    <label for="type" class="form-label">Type</label>
                    <select id="type" v-model="videoForm.type" class="form-input" required>
                      <option value="EPISODE">Episode</option>
                      <option value="MOVIE">Movie</option>
                      <option value="OTHER">Other</option>
                    </select>
                  </div>
                  
                  <div class="form-group">
                    <label for="genre" class="form-label">Genre</label>
                    <input
                      id="genre"
                      v-model="videoForm.genre"
                      type="text"
                      class="form-input"
                    />
                  </div>
                  
                  <div class="form-group">
                    <label for="length" class="form-label">Length (seconds)</label>
                    <input
                      id="length"
                      v-model.number="videoForm.length"
                      type="number"
                      class="form-input"
                      required
                    />
                  </div>
                  
                  <div v-if="videoForm.type === 'EPISODE'" class="form-group">
                    <label for="seasonNumber" class="form-label">Season Number</label>
                    <input
                      id="seasonNumber"
                      v-model.number="videoForm.seasonNumber"
                      type="number"
                      class="form-input"
                    />
                  </div>
                  
                  <div v-if="videoForm.type === 'EPISODE'" class="form-group">
                    <label for="episodeNumber" class="form-label">Episode Number</label>
                    <input
                      id="episodeNumber"
                      v-model.number="videoForm.episodeNumber"
                      type="number"
                      class="form-input"
                    />
                  </div>
                </div>
                
                <div class="form-group">
                  <label for="description" class="form-label">Description</label>
                  <textarea
                    id="description"
                    v-model="videoForm.description"
                    class="form-input"
                    rows="4"
                    style="resize: vertical"
                  ></textarea>
                </div>
                
                <div class="form-group">
                  <label for="playlist" class="form-label">Add to Playlist (optional)</label>
                  <select id="playlist" v-model="videoForm.playlistId" class="form-input">
                    <option value="">None</option>
                    <option v-for="playlist in playlists" :key="playlist.id" :value="playlist.id">
                      {{ playlist.name }}
                    </option>
                  </select>
                </div>

                <!-- Enhanced Encryption Section -->
                <div class="form-group encryption-section">
                  <div class="encryption-header">
                    <div class="encryption-toggle">
                      <label class="checkbox-label">
                        <input 
                          type="checkbox" 
                          id="encrypted" 
                          v-model="videoForm.encrypted"
                          @change="handleEncryptionToggle"
                        >
                        <BaseIcon name="lock" :size="16" />
                        <span>Encrypt Video</span>
                      </label>
                      <button 
                        v-if="videoForm.encrypted" 
                        type="button" 
                        @click="showEncryptionInfo = !showEncryptionInfo"
                        class="info-toggle-btn"
                      >
                        <BaseIcon :name="showEncryptionInfo ? 'chevron-up' : 'chevron-down'" :size="16" />
                      </button>
                    </div>
                    <router-link to="/encryption-manager" class="key-manager-link">
                      <BaseIcon name="key" :size="16" />
                      Manage All Keys
                    </router-link>
                  </div>
                  
                  <div v-if="videoForm.encrypted" class="encryption-content">
                    <div v-if="showEncryptionInfo" class="encryption-info">
                      <BaseIcon name="info" :size="16" />
                      <p>Video encryption protects your content with a secure key. Only users with the key can view the video.</p>
                    </div>

                    <div class="encryption-options">
                      <label class="radio-label">
                        <input 
                          type="radio" 
                          v-model="encryptionMode" 
                          value="auto"
                          @change="handleEncryptionModeChange"
                        >
                        <span>Generate key automatically</span>
                      </label>
                      <label class="radio-label">
                        <input 
                          type="radio" 
                          v-model="encryptionMode" 
                          value="manual"
                          @change="handleEncryptionModeChange"
                        >
                        <span>Enter custom key</span>
                      </label>
                    </div>

                    <div v-if="encryptionMode === 'auto'" class="key-display">
                      <div class="key-header">
                        <label class="form-label">Generated Encryption Key</label>
                        <button 
                          type="button" 
                          @click="exportCurrentKey"
                          class="export-key-btn"
                          title="Export this key"
                        >
                          <BaseIcon name="download" :size="14" />
                          Export Key
                        </button>
                      </div>
                      <div class="key-display-group">
                        <input 
                          :type="showGeneratedKey ? 'text' : 'password'"
                          :value="videoForm.encryptionKey"
                          class="form-input key-input"
                          id="generatedKeyInput"
                        >
                        <button 
                          type="button" 
                          @click="showGeneratedKey = !showGeneratedKey"
                          class="icon-btn"
                          title="Toggle visibility"
                        >
                          <BaseIcon :name="showGeneratedKey ? 'eye-off' : 'eye'" :size="16" />
                        </button>
                        <button 
                          type="button" 
                          @click="copyKey"
                          class="icon-btn"
                          title="Copy key"
                        >
                          <BaseIcon name="copy" :size="16" />
                        </button>
                        <button 
                          type="button" 
                          @click="regenerateKey"
                          class="icon-btn"
                          title="Generate new key"
                        >
                          <BaseIcon name="refresh" :size="16" />
                        </button>
                      </div>
                      <p class="key-warning">
                        <BaseIcon name="alert" :size="14" />
                        Save this key securely! It will be stored locally, but you should keep a backup.
                      </p>
                    </div>

                    <div v-if="encryptionMode === 'manual'" class="key-input-section">
                      <label for="encryptionKey" class="form-label">Custom Encryption Key</label>
                      <div class="key-input-group">
                        <input 
                          id="encryptionKey" 
                          :type="showManualKey ? 'text' : 'password'" 
                          class="form-input" 
                          v-model="videoForm.encryptionKey"
                          placeholder="Enter your encryption key"
                        >
                        <button 
                          type="button" 
                          @click="showManualKey = !showManualKey"
                          class="icon-btn"
                          title="Toggle visibility"
                        >
                          <BaseIcon :name="showManualKey ? 'eye-off' : 'eye'" :size="16" />
                        </button>
                      </div>
                    </div>
                  </div>
                </div>
                
                <div class="form-group">
                  <label class="form-label">Video File</label>
                  <div class="file-upload-area">
                    <input
                      ref="fileInput"
                      type="file"
                      accept="video/*"
                      @change="handleFileSelect"
                      class="file-input"
                    />
                    <div class="file-upload-zone" @click="$refs.fileInput.click()">
                      <div v-if="!selectedFile" class="upload-content">
                        <BaseIcon name="upload" :size="48" class="upload-icon" />
                        <p class="upload-text">Click to select video file or drag and drop</p>
                        <p class="upload-hint">Max size: 10GB</p>
                      </div>
                      <div v-else class="selected-file">
                        <BaseIcon name="file" :size="24" class="file-icon" />
                        <div class="file-info">
                          <p class="file-name">{{ selectedFile.name }}</p>
                          <p class="file-size">{{ formatFileSize(selectedFile.size) }}</p>
                        </div>
                        <button @click.stop="selectedFile = null" class="remove-file-btn">
                          <BaseIcon name="close" :size="16" />
                        </button>
                      </div>
                    </div>
                  </div>
                </div>
                
                <div v-if="uploadProgress > 0" class="upload-progress">
                  <div class="progress-bar">
                    <div 
                      class="progress-fill" 
                      :style="{ width: `${uploadProgress}%` }"
                    ></div>
                  </div>
                  <p class="progress-text">{{ uploadProgress }}% uploaded</p>
                </div>
                
                <div v-if="error" class="error-text">{{ error }}</div>
                <div v-if="successMessage" class="success-text">{{ successMessage }}</div>
                
                <div class="form-actions">
                  <button type="submit" class="btn btn-primary" :disabled="isLoading || !selectedFile">
                    {{ isLoading ? 'Uploading...' : 'Upload Video' }}
                  </button>
                </div>
              </form>
            </div>
            
            <div v-else class="tab-content">
              <form @submit.prevent="handlePlaylistCreate" class="upload-form">
                <div class="form-group">
                  <label for="playlistName" class="form-label">Playlist Name</label>
                  <input
                    id="playlistName"
                    v-model="playlistForm.name"
                    type="text"
                    class="form-input"
                    required
                  />
                </div>
                
                <div class="form-group">
                  <label for="playlistDesc" class="form-label">Description</label>
                  <textarea
                    id="playlistDesc"
                    v-model="playlistForm.description"
                    class="form-input"
                    rows="4"
                    style="resize: vertical"
                  ></textarea>
                </div>
                
                <div v-if="error" class="error-text">{{ error }}</div>
                <div v-if="successMessage" class="success-text">{{ successMessage }}</div>
                
                <div class="form-actions">
                  <button type="submit" class="btn btn-primary" :disabled="isLoading">
                    {{ isLoading ? 'Creating...' : 'Create Playlist' }}
                  </button>
                </div>
              </form>
            </div>
          </div>
        </div>
      </div>
    </div>
  </template>
  
  <script setup>
  import { ref, onMounted, computed } from 'vue'
  import { useRouter } from 'vue-router'
  import { useVideoStore } from '../stores/video'
  import BaseIcon from '../components/icons/BaseIcon.vue'
  import encryptionKeyService from '@/services/encryptionKeyService'
  
  const videoStore = useVideoStore()
  const router = useRouter()
  
  const activeTab = ref('video')
  const playlists = ref([])
  const selectedFile = ref(null)
  const fileInput = ref(null)
  const isLoading = ref(false)
  const error = ref('')
  const successMessage = ref('')
  
  // Encryption related refs
  const encryptionMode = ref('auto')
  const showGeneratedKey = ref(false)
  const showManualKey = ref(false)
  const showEncryptionInfo = ref(false)
  
  const videoForm = ref({
    title: '',
    type: 'EPISODE',
    length: 0,
    genre: '',
    description: '',
    seasonNumber: 1,
    episodeNumber: 1,
    playlistId: '',
    encrypted: false,
    encryptionKey: null
  })
  
  const playlistForm = ref({
    name: '',
    description: ''
  })
  
  const uploadProgress = computed(() => videoStore.uploadProgress)
  
  onMounted(async () => {
    try {
      await videoStore.getMyPlaylists()
      playlists.value = videoStore.playlists
    } catch (err) {
      console.error('Failed to load playlists:', err)
    }
  })
  
  function handleFileSelect(event) {
    const file = event.target.files[0]
    if (file) {
      selectedFile.value = file
    }
  }
  
  function formatFileSize(bytes) {
    if (bytes === 0) return '0 Bytes'
    const k = 1024
    const sizes = ['Bytes', 'KB', 'MB', 'GB', 'TB']
    const i = Math.floor(Math.log(bytes) / Math.log(k))
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
  }

  function handleEncryptionToggle() {
    if (videoForm.value.encrypted && encryptionMode.value === 'auto') {
      // Generate a new key when encryption is enabled
      videoForm.value.encryptionKey = encryptionKeyService.generateKey()
    } else if (!videoForm.value.encrypted) {
      // Clear the key when encryption is disabled
      videoForm.value.encryptionKey = null
      showGeneratedKey.value = false
      showManualKey.value = false
    }
  }

  function handleEncryptionModeChange() {
    if (encryptionMode.value === 'auto') {
      // Generate a new key when switching to auto mode
      videoForm.value.encryptionKey = encryptionKeyService.generateKey()
    } else {
      // Clear the key when switching to manual mode
      videoForm.value.encryptionKey = ''
    }
  }

  function regenerateKey() {
    videoForm.value.encryptionKey = encryptionKeyService.generateKey()
  }

  async function copyKey() {
    if (!videoForm.value.encryptionKey) return

    try {
      await navigator.clipboard.writeText(videoForm.value.encryptionKey)
      
      // Show temporary success message
      const originalSuccess = successMessage.value
      successMessage.value = 'Encryption key copied to clipboard!'
      setTimeout(() => {
        if (successMessage.value === 'Encryption key copied to clipboard!') {
          successMessage.value = originalSuccess
        }
      }, 3000)
    } catch (err) {
      console.error('Failed to copy key:', err)
      error.value = 'Failed to copy encryption key'
    }
  }
  
  function exportCurrentKey() {
    if (!videoForm.value.encryptionKey) return
    
    try {
      // Create a single-key JSON object in the same format as the multi-key export
      const keyData = {
        key: videoForm.value.encryptionKey,
        createdAt: new Date().toISOString(),
        lastUsed: new Date().toISOString()
      }
      
      const keyExport = {
        "temp-video-id": keyData // Use a temporary ID since we don't have a real video ID yet
      }
      
      // Convert to JSON and create downloadable blob
      const keysJson = JSON.stringify(keyExport, null, 2)
      const blob = new Blob([keysJson], { type: 'application/json' })
      const url = URL.createObjectURL(blob)
      
      // Create and trigger download
      const a = document.createElement('a')
      a.href = url
      a.download = `spillway-encryption-key-${new Date().toISOString().split('T')[0]}.json`
      document.body.appendChild(a)
      a.click()
      document.body.removeChild(a)
      URL.revokeObjectURL(url)
      
      // Show success message
      const originalSuccess = successMessage.value
      successMessage.value = 'Encryption key exported successfully!'
      setTimeout(() => {
        if (successMessage.value === 'Encryption key exported successfully!') {
          successMessage.value = originalSuccess
        }
      }, 3000)
    } catch (err) {
      console.error('Failed to export key:', err)
      error.value = 'Failed to export encryption key'
    }
  }
  
  async function handleVideoUpload() {
    if (!selectedFile.value) {
      error.value = 'Please select a video file'
      return
    }

    // Validate encryption key if encryption is enabled
    if (videoForm.value.encrypted && !videoForm.value.encryptionKey) {
      error.value = 'Please provide an encryption key'
      return
    }
    
    isLoading.value = true
    error.value = ''
    successMessage.value = ''
    
    try {
      console.log(videoForm.value)
      // Create video metadata
      const videoResult = await videoStore.createVideo(videoForm.value)
      
      if (!videoResult.success) {
        error.value = videoResult.error
        return
      }
      
      // Upload video file
      const uploadResult = await videoStore.uploadVideoFile(
        videoResult.video.id, 
        selectedFile.value, 
        videoForm.value.encryptionKey
      )
      
      if (!uploadResult.success) {
        error.value = uploadResult.error
        return
      }

      // Store encryption key locally if video is encrypted
      if (videoForm.value.encrypted && videoForm.value.encryptionKey) {
        encryptionKeyService.storeKey(videoResult.video.id, videoForm.value.encryptionKey)
      }
      
      successMessage.value = 'Video uploaded successfully! Redirecting...'
      
      setTimeout(() => {
        router.push(`/video/${videoResult.video.id}`)
      }, 2000)
      
    } catch (err) {
      error.value = 'Upload failed: ' + err.message
    } finally {
      isLoading.value = false
    }
  }
  
  async function handlePlaylistCreate() {
    isLoading.value = true
    error.value = ''
    successMessage.value = ''
    
    const result = await videoStore.createPlaylist(playlistForm.value)
    
    if (result.success) {
      successMessage.value = 'Playlist created successfully!'
      playlistForm.value = {
        name: '',
        description: ''
      }
      playlists.value.unshift(result.playlist)
    } else {
      error.value = result.error
    }
    
    isLoading.value = false
  }
  </script>
  
  <style scoped>
    .upload-icon {
    color: var(--accent-color);
    margin-bottom: 1rem;
    transition: var(--transition);
  }

  .file-upload-zone:hover .upload-icon {
    transform: scale(1.1) rotate(-5deg);
  }

  .file-icon {
    color: var(--accent-color);
  }

  .remove-file-btn {
    background: none;
    border: none;
    color: var(--secondary-text);
    cursor: pointer;
    transition: var(--transition);
    padding: 0.5rem;
    display: flex;
    align-items: center;
    justify-content: center;
  }

  .remove-file-btn:hover {
    color: var(--danger-color);
    transform: scale(1.1);
  }

  .upload-container {
    max-width: 800px;
    margin: 0 auto;
  }
  
  .upload-card {
    padding: 2rem;
  }
  
  .upload-tabs {
    display: flex;
    border-bottom: 1px solid var(--border-color);
    margin-bottom: 2rem;
  }
  
  .tab-button {
    background: none;
    border: none;
    color: var(--secondary-text);
    font-size: 1rem;
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
  
  .upload-form {
    display: flex;
    flex-direction: column;
    gap: 1.5rem;
  }
  
  .form-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
    gap: 1.5rem;
  }
  
  .form-group {
    display: flex;
    flex-direction: column;
  }

  /* Encryption Section Styles */
  .encryption-section {
    background-color: var(--tertiary-bg);
    padding: 1.25rem;
    border-radius: 0.75rem;
    border: 1px solid var(--border-color);
  }

  .encryption-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .encryption-toggle {
    display: flex;
    align-items: center;
    gap: 0.75rem;
  }

  .key-manager-link {
    display: flex;
    align-items: center;
    gap: 0.5rem;
    color: var(--accent-color);
    font-size: 0.875rem;
    font-weight: 500;
    text-decoration: none;
    padding: 0.375rem 0.75rem;
    border-radius: 0.375rem;
    transition: var(--transition);
    background-color: rgba(59, 130, 246, 0.1);
  }

  .key-manager-link:hover {
    background-color: rgba(59, 130, 246, 0.2);
    text-decoration: none;
  }

  .checkbox-label {
    display: flex;
    align-items: center;
    gap: 0.5rem;
    cursor: pointer;
    font-weight: 500;
    color: var(--primary-text);
  }

  .checkbox-label input[type="checkbox"] {
    width: 1.25rem;
    height: 1.25rem;
    cursor: pointer;
  }

  .info-toggle-btn {
    background: none;
    border: none;
    color: var(--secondary-text);
    cursor: pointer;
    padding: 0.25rem;
    transition: var(--transition);
  }

  .info-toggle-btn:hover {
    color: var(--primary-text);
  }

  .encryption-content {
    margin-top: 1rem;
  }

  .encryption-info {
    display: flex;
    align-items: flex-start;
    gap: 0.5rem;
    padding: 0.75rem;
    background-color: rgba(59, 130, 246, 0.1);
    border-radius: 0.5rem;
    margin-bottom: 1rem;
    color: var(--accent-color);
    font-size: 0.875rem;
  }

  .encryption-info p {
    margin: 0;
  }

  .encryption-options {
    display: flex;
    gap: 1.5rem;
    margin-bottom: 1rem;
  }

  .radio-label {
    display: flex;
    align-items: center;
    gap: 0.5rem;
    cursor: pointer;
    font-size: 0.9375rem;
    color: var(--primary-text);
  }

  .radio-label input[type="radio"] {
    cursor: pointer;
  }

  .key-display {
    margin-top: 1rem;
  }

  .key-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 0.5rem;
  }

  .export-key-btn {
    display: flex;
    align-items: center;
    gap: 0.375rem;
    font-size: 0.8125rem;
    padding: 0.375rem 0.75rem;
    background-color: var(--tertiary-bg);
    color: var(--secondary-text);
    border: none;
    border-radius: 0.375rem;
    cursor: pointer;
    transition: var(--transition);
  }

  .export-key-btn:hover {
    background-color: var(--hover-bg);
    color: var(--primary-text);
  }

  .key-display-group {
    display: flex;
    gap: 0.5rem;
    margin-bottom: 0.5rem;
  }

  .key-input {
    flex: 1;
    font-family: monospace;
    font-size: 0.875rem;
  }

  .key-input-section {
    margin-top: 1rem;
  }

  .key-input-group {
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

  .key-warning {
    display: flex;
    align-items: center;
    gap: 0.5rem;
    padding: 0.5rem;
    background-color: rgba(251, 191, 36, 0.1);
    border-radius: 0.375rem;
    color: #f59e0b;
    font-size: 0.8125rem;
    margin: 0;
  }
  
  .file-upload-area {
    position: relative;
  }
  
  .file-input {
    display: none;
  }
  
  .file-upload-zone {
    border: 2px dashed var(--border-color);
    border-radius: 0.75rem;
    padding: 2rem;
    text-align: center;
    cursor: pointer;
    transition: var(--transition);
    min-height: 150px;
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
    gap: 0.5rem;
  }
  
  .upload-icon {
    font-size: 2rem;
    margin-bottom: 0.5rem;
  }
  
  .upload-text {
    color: var(--primary-text);
    font-weight: 500;
  }
  
  .upload-hint {
    color: var(--secondary-text);
    font-size: 0.875rem;
  }
  
  .selected-file {
    display: flex;
    align-items: center;
    gap: 1rem;
    padding: 1rem;
    background-color: var(--tertiary-bg);
    border-radius: 0.5rem;
    width: 100%;
  }
  
  .file-icon {
    font-size: 1.5rem;
  }
  
  .file-info {
    flex: 1;
    text-align: left;
  }
  
  .file-name {
    font-weight: 500;
  }
  
  .file-size {
    color: var(--secondary-text);
    font-size: 0.875rem;
  }
  
  .remove-file-btn {
    background: none;
    border: none;
    color: var(--secondary-text);
    font-size: 1.5rem;
    cursor: pointer;
    line-height: 1;
    transition: var(--transition);
  }
  
  .remove-file-btn:hover {
    color: var(--danger-color);
  }
  
  .upload-progress {
    margin-top: 1rem;
  }
  
  .progress-bar {
    height: 8px;
    background-color: var(--secondary-bg);
    border-radius: 4px;
    overflow: hidden;
  }
  
  .progress-fill {
    height: 100%;
    background-color: var(--accent-color);
    transition: width 0.3s ease;
  }
  
  .progress-text {
    text-align: center;
    margin-top: 0.5rem;
    color: var(--secondary-text);
    font-size: 0.875rem;
  }
  
  .form-actions {
    display: flex;
    justify-content: flex-end;
    gap: 1rem;
    margin-top: 1rem;
  }
  
  @media (max-width: 768px) {
    .form-grid {
      grid-template-columns: 1fr;
    }

    .encryption-options {
      flex-direction: column;
      gap: 0.75rem;
    }

    .key-display-group {
      flex-wrap: wrap;
    }

    .icon-btn {
      flex: 0 0 auto;
    }
  }
  </style>