<template>
    <div class="modal-overlay" @click="closeModal">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h2>Edit Video</h2>
          <button @click="closeModal" class="close-btn">
            <BaseIcon name="close" :size="20" />
          </button>
        </div>
        
        <form @submit.prevent="handleSubmit" class="edit-form">
          <div class="form-group">
            <label class="form-label">Title</label>
            <input v-model="form.title" type="text" class="form-input" required />
          </div>
          
          <div class="form-group">
            <label class="form-label">Genre</label>
            <input v-model="form.genre" type="text" class="form-input" />
          </div>
          
          <div class="form-group">
            <label class="form-label">Description</label>
            <textarea v-model="form.description" class="form-input" rows="4"></textarea>
          </div>
          
          <div v-if="error" class="error-text">{{ error }}</div>
          
          <div class="modal-actions">
            <button type="button" @click="closeModal" class="btn btn-secondary">
              Cancel
            </button>
            <button type="submit" class="btn btn-primary" :disabled="isLoading">
              {{ isLoading ? 'Saving...' : 'Save Changes' }}
            </button>
          </div>
        </form>
      </div>
    </div>
  </template>
  
  <script setup>
  import { ref, reactive } from 'vue'
  import { useVideoStore } from '../stores/video'
  import BaseIcon from './icons/BaseIcon.vue'
  
  const props = defineProps({
    video: {
      type: Object,
      required: true
    }
  })
  
  const emit = defineEmits(['close', 'updated'])
  
  const videoStore = useVideoStore()
  const isLoading = ref(false)
  const error = ref('')
  
  const form = reactive({
    title: props.video.title,
    genre: props.video.genre || '',
    description: props.video.description || ''
  })
  
  function closeModal() {
    emit('close')
  }
  
  async function handleSubmit() {
    isLoading.value = true
    error.value = ''
    
    const result = await videoStore.updateVideo(props.video.id, form)
    
    if (result.success) {
      emit('updated', result.video)
      closeModal()
    } else {
      error.value = result.error
    }
    
    isLoading.value = false
  }
  </script>
  
  <style scoped>
  .modal-overlay {
    position: fixed;
    inset: 0;
    background-color: rgba(0, 0, 0, 0.7);
    display: flex;
    align-items: center;
    justify-content: center;
    z-index: 1000;
  }
  
  .modal-content {
    background-color: var(--secondary-bg);
    border-radius: 1rem;
    width: 90%;
    max-width: 500px;
    max-height: 90vh;
    overflow-y: auto;
    box-shadow: var(--shadow);
  }
  
  .modal-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 1.5rem;
    border-bottom: 1px solid var(--border-color);
  }
  
  .close-btn {
    background: none;
    border: none;
    color: var(--secondary-text);
    cursor: pointer;
    padding: 0.5rem;
    border-radius: 0.5rem;
    transition: var(--transition);
  }
  
  .close-btn:hover {
    background-color: var(--tertiary-bg);
  }
  
  .edit-form {
    padding: 1.5rem;
  }
  
  .modal-actions {
    display: flex;
    gap: 1rem;
    justify-content: flex-end;
    margin-top: 2rem;
  }
  </style>