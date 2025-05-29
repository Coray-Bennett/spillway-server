<template>
  <div class="pagination" v-if="totalPages > 1">
    <AppButton 
      @click="changePage(currentPage - 1)" 
      :disabled="currentPage === 0"
      variant="secondary"
      size="small"
      icon="chevron-left"
      v-if="!simple || currentPage > 0"
    >
      {{ previousLabel }}
    </AppButton>
    
    <template v-if="!simple">
      <!-- First page button -->
      <AppButton 
        v-if="showFirst"
        @click="changePage(0)" 
        variant="text"
        size="small"
      >
        1
      </AppButton>
      
      <span v-if="startPage > 1" class="pagination-ellipsis">...</span>
      
      <!-- Page numbers -->
      <AppButton 
        v-for="page in pageRange" 
        :key="page"
        @click="changePage(page)" 
        :variant="page === currentPage ? 'primary' : 'text'"
        size="small"
        class="pagination-number"
      >
        {{ page + 1 }}
      </AppButton>
      
      <span v-if="endPage < totalPages - 2" class="pagination-ellipsis">...</span>
      
      <!-- Last page button -->
      <AppButton 
        v-if="showLast"
        @click="changePage(totalPages - 1)" 
        variant="text"
        size="small"
      >
        {{ totalPages }}
      </AppButton>
    </template>
    
    <span v-else class="page-info">
      {{ currentPage + 1 }} / {{ totalPages }}
    </span>
    
    <AppButton 
      @click="changePage(currentPage + 1)" 
      :disabled="currentPage >= totalPages - 1"
      variant="secondary"
      size="small"
      icon="chevron-right"
      icon-position="right"
      v-if="!simple || currentPage < totalPages - 1"
    >
      {{ nextLabel }}
    </AppButton>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import AppButton from './AppButton.vue'

const props = defineProps({
  currentPage: {
    type: Number,
    required: true
  },
  totalPages: {
    type: Number,
    required: true
  },
  visiblePages: {
    type: Number,
    default: 5
  },
  simple: {
    type: Boolean,
    default: false
  },
  previousLabel: {
    type: String,
    default: 'Previous'
  },
  nextLabel: {
    type: String,
    default: 'Next'
  }
})

const emit = defineEmits(['page-change'])

// Calculate range of page buttons to display
const startPage = computed(() => {
  // Show pages around current page
  let start = Math.max(0, props.currentPage - Math.floor(props.visiblePages / 2))
  // Adjust if we're near the end to maintain the same number of visible pages
  const end = Math.min(props.totalPages - 1, start + props.visiblePages - 1)
  // If we adjusted end, we may need to readjust start
  start = Math.max(0, Math.min(start, end - props.visiblePages + 1))
  return start
})

const endPage = computed(() => {
  return Math.min(props.totalPages - 1, startPage.value + props.visiblePages - 1)
})

const pageRange = computed(() => {
  const range = []
  for (let i = startPage.value; i <= endPage.value; i++) {
    range.push(i)
  }
  return range
})

// Determine if we need to show first/last page buttons
const showFirst = computed(() => startPage.value > 0)
const showLast = computed(() => endPage.value < props.totalPages - 1)

function changePage(page) {
  if (page >= 0 && page < props.totalPages) {
    emit('page-change', page)
  }
}
</script>

<style scoped>
.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  margin: 2rem 0;
  gap: 0.5rem;
  flex-wrap: wrap;
}

.pagination-ellipsis {
  color: var(--text-secondary, #b0b0b0);
  margin: 0 0.25rem;
}

.pagination-number {
  min-width: 2rem;
}

.page-info {
  padding: 0 1rem;
  color: var(--text-secondary, #b0b0b0);
  font-size: 0.9rem;
}

@media (max-width: 640px) {
  .pagination {
    gap: 0.25rem;
  }
}
</style>