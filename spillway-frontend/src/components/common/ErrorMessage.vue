<template>
  <div v-if="message" class="error-message" :class="classes">
    <slot>{{ message }}</slot>
    <button v-if="dismissible" class="dismiss-btn" @click="$emit('dismiss')" aria-label="Dismiss">
      &times;
    </button>
  </div>
</template>

<script setup>
defineProps({
  message: {
    type: String,
    default: ''
  },
  variant: {
    type: String,
    default: 'error',
    validator: (value) => ['error', 'warning', 'info'].includes(value)
  },
  dismissible: {
    type: Boolean,
    default: false
  }
})

defineEmits(['dismiss'])

// Computed property converted to a simple variable in setup script
const classes = {
  'error-message--error': props.variant === 'error',
  'error-message--warning': props.variant === 'warning',
  'error-message--info': props.variant === 'info'
}
</script>

<style scoped>
.error-message {
  padding: 0.8rem;
  border-radius: 4px;
  margin-bottom: 1rem;
  position: relative;
}

.error-message--error {
  background-color: rgba(251, 86, 7, 0.1);
  color: var(--accent-danger, #fb5607);
  border: 1px solid rgba(251, 86, 7, 0.3);
}

.error-message--warning {
  background-color: rgba(246, 190, 0, 0.1);
  color: var(--accent-warning, #f6be00);
  border: 1px solid rgba(246, 190, 0, 0.3);
}

.error-message--info {
  background-color: rgba(58, 134, 255, 0.1);
  color: var(--accent-primary, #3a86ff);
  border: 1px solid rgba(58, 134, 255, 0.3);
}

.dismiss-btn {
  position: absolute;
  top: 0.5rem;
  right: 0.5rem;
  background: transparent;
  border: none;
  font-size: 1.2rem;
  cursor: pointer;
  color: inherit;
  padding: 0;
  line-height: 1;
}
</style>