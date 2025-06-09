<template>
  <div class="modal-overlay" @click.self="$emit('close')">
    <div class="modal-content">
      <div class="modal-header">
        <h3>Create New Playlist</h3>
        <button @click="$emit('close')" class="close-btn">
          <BaseIcon name="x" :size="20" />
        </button>
      </div>

      <div class="modal-body">
        <form @submit.prevent="createPlaylist" class="playlist-form">
          <div class="form-group">
            <label for="playlist-name">Playlist Name</label>
            <input
              id="playlist-name"
              v-model="form.name"
              type="text"
              class="form-input"
              placeholder="Enter playlist name"
              required
              maxlength="100"
            />
            <div class="char-count">{{ form.name.length }}/100 characters</div>
          </div>

          <div class="form-group">
            <label for="playlist-description">Description (optional)</label>
            <textarea
              id="playlist-description"
              v-model="form.description"
              class="form-textarea"
              placeholder="Describe your playlist..."
              rows="4"
              maxlength="500"
            ></textarea>
            <div class="char-count">{{ (form.description || '').length }}/500 characters</div>
          </div>

          <div class="form-actions">
            <button type="button" @click="$emit('close')" class="btn btn-secondary">
              Cancel
            </button>
            <button 
              type="submit" 
              :disabled="playlistStore.isLoading || !form.name.trim()"
              class="btn btn-primary"
            >
              <span v-if="playlistStore.isLoading">Creating...</span>
              <span v-else>Create Playlist</span>
            </button>
          </div>
        </form>

        <!-- Error Message -->
        <div v-if="playlistStore.error" class="error-message">
          {{ playlistStore.error }}
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { usePlaylistStore } from '@/stores/playlist'
import BaseIcon from './icons/BaseIcon.vue'

const emit = defineEmits(['close', 'created'])

const playlistStore = usePlaylistStore()

const form = reactive({
  name: '',
  description: ''
})

async function createPlaylist() {
  const playlistData = {
    name: form.name.trim(),
    description: form.description?.trim() || null
  }

  const result = await playlistStore.createPlaylist(playlistData)
  
  if (result.success) {
    // Reset form
    form.name = ''
    form.description = ''
    
    emit('created', result.playlist)
    emit('close')
  }
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
  max-width: 500px;
  width: 100%;
  max-height: 90vh;
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

.playlist-form {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.form-group label {
  font-weight: 500;
  color: var(--primary-text);
}

.form-input, .form-textarea {
  padding: 0.75rem;
  border: 1px solid var(--border-color);
  border-radius: 0.5rem;
  background-color: var(--primary-bg);
  color: var(--primary-text);
  font-family: inherit;
  transition: var(--transition);
  resize: vertical;
}

.form-input:focus, .form-textarea:focus {
  outline: none;
  border-color: var(--accent-color);
  box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.1);
}

.char-count {
  font-size: 0.75rem;
  color: var(--secondary-text);
  text-align: right;
}

.form-actions {
  display: flex;
  gap: 1rem;
  justify-content: flex-end;
  margin-top: 1rem;
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
}
</style>