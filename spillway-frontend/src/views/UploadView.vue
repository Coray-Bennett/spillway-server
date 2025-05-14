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
  
  const videoStore = useVideoStore()
  const router = useRouter()
  
  const activeTab = ref('video')
  const playlists = ref([])
  const selectedFile = ref(null)
  const fileInput = ref(null)
  const isLoading = ref(false)
  const error = ref('')
  const successMessage = ref('')
  
  const videoForm = ref({
    title: '',
    type: 'EPISODE',
    length: 0,
    genre: '',
    description: '',
    seasonNumber: 1,
    episodeNumber: 1,
    playlistId: ''
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
  
  async function handleVideoUpload() {
    if (!selectedFile.value) {
      error.value = 'Please select a video file'
      return
    }
    
    isLoading.value = true
    error.value = ''
    successMessage.value = ''
    
    try {
      // Create video metadata
      const videoResult = await videoStore.createVideo(videoForm.value)
      
      if (!videoResult.success) {
        error.value = videoResult.error
        return
      }
      
      // Upload video file
      const uploadResult = await videoStore.uploadVideoFile(videoResult.video.id, selectedFile.value)
      
      if (!uploadResult.success) {
        error.value = uploadResult.error
        return
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
  }
  </style>