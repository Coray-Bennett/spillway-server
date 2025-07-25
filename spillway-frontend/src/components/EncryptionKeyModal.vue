<template>
  <div class="modal-overlay" @click.self="$emit('close')">
    <div class="modal-container">
      <div class="modal-header">
        <h2 class="modal-title">
          <BaseIcon name="lock" :size="24" />
          Encryption Key Required
        </h2>
        <button @click="$emit('close')" class="close-btn">
          <BaseIcon name="close" :size="20" />
        </button>
      </div>

      <div class="modal-body">
        <div class="info-message">
          <BaseIcon name="info" :size="20" />
          <p>This video is encrypted. Please provide the encryption key to access it.</p>
        </div>

        <div class="form-group">
          <label for="encryptionKey" class="form-label">Encryption Key</label>
          <div class="input-group">
            <input
              id="encryptionKey"
              v-model="encryptionKey"
              :type="showKey ? 'text' : 'password'"
              class="form-input"
              placeholder="Enter encryption key"
              @keyup.enter="handleSubmit"
              ref="keyInput"
            />
            <button @click="showKey = !showKey" class="toggle-visibility-btn">
              <BaseIcon :name="showKey ? 'eye-off' : 'eye'" :size="20" />
            </button>
          </div>
        </div>

        <div class="form-group">
          <label class="checkbox-label">
            <input
              type="checkbox"
              v-model="rememberKey"
              class="checkbox-input"
            />
            <span>Remember this key for future use</span>
          </label>
        </div>

        <div v-if="error" class="error-message">
          <BaseIcon name="error" :size="16" />
          {{ error }}
        </div>

        <div class="key-hints">
          <p class="hint-title">Forgot your key?</p>
          <ul class="hint-list">
            <li>Check with the video owner for the encryption key</li>
            <li>Look for the key in your password manager</li>
            <li>Check any secure notes where you might have saved it</li>
          </ul>
        </div>
      </div>

      <div class="modal-footer">
        <button @click="$emit('close')" class="btn btn-secondary">
          Cancel
        </button>
        <button 
          @click="handleSubmit" 
          class="btn btn-primary"
          :disabled="!encryptionKey.trim()"
        >
          <BaseIcon name="unlock" :size="16" />
          Unlock Video
        </button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
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
  }
})

const emit = defineEmits(['close', 'submit'])

const encryptionKey = ref('')
const rememberKey = ref(true)
const showKey = ref(false)
const error = ref('')
const keyInput = ref(null)

onMounted(() => {
  // Focus the input field when modal opens
  if (keyInput.value) {
    keyInput.value.focus()
  }
})

function handleSubmit() {
  const key = encryptionKey.value.trim()
  
  if (!key) {
    error.value = 'Please enter an encryption key'
    return
  }

  // Store the key if remember option is checked
  if (rememberKey.value) {
    encryptionKeyService.storeKey(props.videoId, key)
  }

  emit('submit', key)
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
  max-width: 500px;
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

.info-message {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 1rem;
  background-color: rgba(59, 130, 246, 0.1);
  border: 1px solid rgba(59, 130, 246, 0.3);
  border-radius: 0.5rem;
  margin-bottom: 1.5rem;
  color: var(--accent-color);
}

.info-message p {
  margin: 0;
  font-size: 0.9375rem;
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

.form-input:focus {
  outline: none;
  border-color: var(--accent-color);
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.1);
}

.form-input::placeholder {
  color: var(--secondary-text);
}

.toggle-visibility-btn {
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

.toggle-visibility-btn:hover {
  background-color: var(--hover-bg);
  color: var(--primary-text);
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

.error-message {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.75rem 1rem;
  background-color: rgba(239, 68, 68, 0.1);
  border: 1px solid rgba(239, 68, 68, 0.3);
  border-radius: 0.5rem;
  margin-bottom: 1.5rem;
  color: var(--danger-color);
  font-size: 0.875rem;
}

.key-hints {
  background-color: var(--tertiary-bg);
  border-radius: 0.5rem;
  padding: 1rem;
  margin-top: 1.5rem;
}

.hint-title {
  font-weight: 500;
  color: var(--primary-text);
  margin: 0 0 0.5rem 0;
  font-size: 0.875rem;
}

.hint-list {
  margin: 0;
  padding-left: 1.25rem;
  color: var(--secondary-text);
  font-size: 0.875rem;
}

.hint-list li {
  margin-bottom: 0.25rem;
}

.hint-list li:last-child {
  margin-bottom: 0;
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

@media (max-width: 640px) {
  .modal-container {
    max-height: 100vh;
    border-radius: 0;
  }

  .modal-footer {
    flex-direction: column-reverse;
  }

  .btn {
    width: 100%;
    justify-content: center;
  }
}
</style>