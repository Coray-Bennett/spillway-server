<template>
  <div class="empty-state" :class="{ [`empty-state--${variant}`]: true }">
    <BaseIcon v-if="icon" :name="icon" :size="iconSize" class="empty-icon" />
    <div v-if="imageUrl" class="empty-state-image">
      <img :src="imageUrl" :alt="title" />
    </div>
    
    <h3 v-if="title" class="empty-state-title">{{ title }}</h3>
    <p v-if="description" class="empty-state-description">{{ description }}</p>
    
    <div class="empty-state-content">
      <slot></slot>
    </div>
    
    <div v-if="$slots.actions" class="empty-state-actions">
      <slot name="actions"></slot>
    </div>
  </div>
</template>

<script setup>
import BaseIcon from '../icons/BaseIcon.vue'

defineProps({
  title: {
    type: String,
    default: ''
  },
  description: {
    type: String,
    default: ''
  },
  icon: {
    type: String,
    default: ''
  },
  iconSize: {
    type: Number,
    default: 48
  },
  imageUrl: {
    type: String,
    default: ''
  },
  variant: {
    type: String,
    default: 'default',
    validator: (value) => ['default', 'compact', 'fullscreen'].includes(value)
  }
})
</script>

<style scoped>
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  padding: 3rem 2rem;
  background-color: var(--card-bg, #2a2a2a);
  border-radius: 8px;
  width: 100%;
}

.empty-state--compact {
  padding: 1.5rem 1rem;
}

.empty-state--fullscreen {
  min-height: 70vh;
}

.empty-icon {
  color: var(--text-muted, #888);
  opacity: 0.7;
  margin-bottom: 1.5rem;
}

.empty-state-image {
  max-width: 200px;
  margin-bottom: 1.5rem;
}

.empty-state-image img {
  width: 100%;
  height: auto;
}

.empty-state-title {
  font-size: 1.5rem;
  margin: 0 0 0.5rem;
  color: var(--text-primary, #e0e0e0);
}

.empty-state-description {
  font-size: 1rem;
  margin: 0 0 1.5rem;
  color: var(--text-secondary, #b0b0b0);
  max-width: 500px;
}

.empty-state-content {
  margin-bottom: 1.5rem;
}

.empty-state-actions {
  display: flex;
  gap: 1rem;
  flex-wrap: wrap;
  justify-content: center;
}

@media (max-width: 640px) {
  .empty-state {
    padding: 2rem 1rem;
  }
  
  .empty-state-title {
    font-size: 1.25rem;
  }
  
  .empty-state-description {
    font-size: 0.9rem;
  }
  
  .empty-state-actions {
    flex-direction: column;
    width: 100%;
    gap: 0.5rem;
  }
}
</style>