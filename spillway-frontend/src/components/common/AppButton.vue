<template>
  <component
    :is="tag"
    class="app-button"
    :class="buttonClasses"
    :type="type"
    :disabled="disabled || loading"
    v-bind="$attrs"
  >
    <LoadingSpinner v-if="loading" :size="1" inline />
    <BaseIcon v-if="icon && !loading" :name="icon" :size="iconSize" class="btn-icon" />
    <slot></slot>
  </component>
</template>

<script setup>
import { computed } from 'vue'
import BaseIcon from '../icons/BaseIcon.vue'
import LoadingSpinner from './LoadingSpinner.vue'

const props = defineProps({
  variant: {
    type: String,
    default: 'primary',
    validator: (value) => ['primary', 'secondary', 'danger', 'success', 'text', 'link'].includes(value)
  },
  size: {
    type: String,
    default: 'medium',
    validator: (value) => ['small', 'medium', 'large'].includes(value)
  },
  type: {
    type: String,
    default: 'button'
  },
  icon: {
    type: String,
    default: ''
  },
  iconSize: {
    type: Number,
    default: 16
  },
  disabled: {
    type: Boolean,
    default: false
  },
  loading: {
    type: Boolean,
    default: false
  },
  tag: {
    type: String,
    default: 'button'
  }
})

const buttonClasses = computed(() => ({
  [`app-button--${props.variant}`]: true,
  [`app-button--${props.size}`]: true,
  'app-button--loading': props.loading,
  'app-button--icon-only': !!props.icon && !$slots.default,
  'app-button--has-icon': !!props.icon && !!$slots.default
}))
</script>

<style scoped>
.app-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  border-radius: 4px;
  font-weight: 500;
  transition: all 0.2s ease;
  cursor: pointer;
  border: 1px solid transparent;
  text-decoration: none;
}

/* Variants */
.app-button--primary {
  background-color: var(--accent-primary, #3a86ff);
  color: white;
}

.app-button--primary:hover:not(:disabled) {
  background-color: var(--button-primary-hover, #2a76ef);
}

.app-button--secondary {
  background-color: var(--button-secondary-bg, #2a2a2a);
  color: var(--text-primary, #e0e0e0);
  border: 1px solid var(--border-color, #333333);
}

.app-button--secondary:hover:not(:disabled) {
  background-color: var(--bg-tertiary, #333333);
}

.app-button--danger {
  background-color: var(--accent-danger, #fb5607);
  color: white;
}

.app-button--danger:hover:not(:disabled) {
  background-color: #e04e06;
}

.app-button--success {
  background-color: var(--accent-success, #25b869);
  color: white;
}

.app-button--success:hover:not(:disabled) {
  background-color: #1fa35c;
}

.app-button--text {
  background-color: transparent;
  color: var(--text-primary, #e0e0e0);
}

.app-button--text:hover:not(:disabled) {
  background-color: rgba(0, 0, 0, 0.05);
}

.app-button--link {
  background-color: transparent;
  color: var(--accent-primary, #3a86ff);
  text-decoration: none;
}

.app-button--link:hover:not(:disabled) {
  text-decoration: underline;
}

/* Sizes */
.app-button--small {
  padding: 0.33rem 0.75rem;
  font-size: 0.85rem;
}

.app-button--medium {
  padding: 0.6rem 1.2rem;
  font-size: 0.95rem;
}

.app-button--large {
  padding: 0.8rem 1.5rem;
  font-size: 1.1rem;
}

/* States */
.app-button:disabled {
  opacity: 0.65;
  cursor: not-allowed;
}

.app-button--loading {
  cursor: wait;
}

/* Icon styles */
.app-button--icon-only {
  padding: 0.5rem;
}

.app-button--icon-only.app-button--small {
  padding: 0.33rem;
}

.app-button--icon-only.app-button--large {
  padding: 0.67rem;
}

.btn-icon {
  transition: transform 0.2s;
}

.app-button:hover:not(:disabled) .btn-icon {
  transform: scale(1.1);
}
</style>
