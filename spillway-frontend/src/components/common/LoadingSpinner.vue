<template>
  <div class="loading-container" :class="{ 'loading-container--inline': inline }">
    <div class="spinner" :style="spinnerStyle"></div>
    <p v-if="message" class="loading-message">{{ message }}</p>
    <slot></slot>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  size: {
    type: Number,
    default: 3
  },
  message: {
    type: String,
    default: ''
  },
  inline: {
    type: Boolean,
    default: false
  },
  color: {
    type: String,
    default: ''
  }
})

const spinnerStyle = computed(() => {
  return {
    width: `${props.size}rem`,
    height: `${props.size}rem`,
    borderTopColor: props.color || 'var(--accent-primary, #3a86ff)'
  }
})
</script>

<style scoped>
.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 1.5rem;
}

.loading-container--inline {
  display: inline-flex;
  flex-direction: row;
  padding: 0;
  gap: 0.5rem;
}

.spinner {
  width: 3rem;
  height: 3rem;
  border: 3px solid rgba(162, 162, 162, 0.3);
  border-radius: 50%;
  border-top-color: var(--accent-primary, #3a86ff);
  animation: spin 1s ease-in-out infinite;
  margin-bottom: 1rem;
}

.loading-container--inline .spinner {
  width: 1rem;
  height: 1rem;
  margin-bottom: 0;
  border-width: 2px;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.loading-message {
  color: var(--text-secondary, #b0b0b0);
  margin: 0;
}
</style>