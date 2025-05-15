<template>
    <div class="playlist-manager">
      <h3>Manage Playlists</h3>
      
      <div class="playlist-actions">
        <select v-model="selectedPlaylistId" class="form-input">
          <option value="">Select a playlist</option>
          <option v-for="playlist in playlists" :key="playlist.id" :value="playlist.id">
            {{ playlist.name }}
          </option>
        </select>
        
        <button 
          @click="handleAddToPlaylist" 
          class="btn btn-primary"
          :disabled="!selectedPlaylistId || isLoading"
        >
          <BaseIcon name="plus" :size="16" />
          Add to Playlist
        </button>
      </div>
      
      <div v-if="video.playlistName" class="current-playlist">
        <p>Currently in: <strong>{{ video.playlistName }}</strong></p>
        <button @click="handleRemoveFromPlaylist" class="btn btn-danger btn-sm">
          Remove from Playlist
        </button>
      </div>
      
      <div v-if="showEpisodeDetails" class="episode-details">
        <div class="form-group">
          <label class="form-label">Season Number</label>
          <input v-model.number="episodeDetails.seasonNumber" type="number" class="form-input" />
        </div>
        
        <div class="form-group">
          <label class="form-label">Episode Number</label>
          <input v-model.number="episodeDetails.episodeNumber" type="number" class="form-input" />
        </div>
      </div>
      
      <div v-if="error" class="error-text">{{ error }}</div>
      <div v-if="successMessage" class="success-text">{{ successMessage }}</div>
    </div>
  </template>
  
  <script setup>
  import { ref, reactive, computed, onMounted } from 'vue'
  import { useVideoStore } from '../stores/video'
  import BaseIcon from './icons/BaseIcon.vue'
  
  const props = defineProps({
    video: {
      type: Object,
      required: true
    }
  })
  
  const emit = defineEmits(['updated'])
  
  const videoStore = useVideoStore()
  const isLoading = ref(false)
  const error = ref('')
  const successMessage = ref('')
  const selectedPlaylistId = ref('')
  const playlists = computed(() => videoStore.playlists)
  
  const episodeDetails = reactive({
    seasonNumber: props.video.seasonNumber,
    episodeNumber: props.video.episodeNumber
  })
  
  const showEpisodeDetails = computed(() => 
    props.video.type === 'EPISODE' && selectedPlaylistId.value
  )
  
  onMounted(async () => {
    await videoStore.getMyPlaylists()
  })
  
  async function handleAddToPlaylist() {
    isLoading.value = true
    error.value = ''
    successMessage.value = ''
    
    const details = props.video.type === 'EPISODE' ? episodeDetails : null
    
    const result = await videoStore.addVideoToPlaylist(
      selectedPlaylistId.value, 
      props.video.id,
      details
    )
    
    if (result.success) {
      successMessage.value = 'Video added to playlist'
      emit('updated')
    } else {
      error.value = result.error
    }
    
    isLoading.value = false
  }
  
  async function handleRemoveFromPlaylist() {
    isLoading.value = true
    error.value = ''
    successMessage.value = ''
    
    const result = await videoStore.removeVideoFromPlaylist(
      props.video.playlist?.id, 
      props.video.id
    )
    
    if (result.success) {
      successMessage.value = 'Video removed from playlist'
      emit('updated')
    } else {
      error.value = result.error
    }
    
    isLoading.value = false
  }
  </script>
  
  <style scoped>
  .playlist-manager {
    padding: 1.5rem;
    background-color: var(--tertiary-bg);
    border-radius: 0.5rem;
  }
  
  .playlist-actions {
    display: flex;
    gap: 1rem;
    margin: 1rem 0;
  }
  
  .current-playlist {
    margin-top: 1rem;
    padding: 1rem;
    background-color: var(--secondary-bg);
    border-radius: 0.5rem;
  }
  
  .episode-details {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 1rem;
    margin-top: 1rem;
  }
  </style>