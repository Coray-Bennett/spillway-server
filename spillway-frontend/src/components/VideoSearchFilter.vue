<template>
  <div class="video-search-filter">
    <div class="search-bar">
      <BaseIcon name="search" :size="16" class="search-icon" />
      <input
        v-model="searchQuery"
        type="text"
        placeholder="Search videos..."
        @keyup.enter="performSearch"
        class="search-input"
      />
      <AppButton
        v-if="searchQuery"
        icon="close"
        variant="text"
        size="small"
        @click="clearSearchInput"
        class="clear-button"
        aria-label="Clear search"
      />
      <AppButton 
        @click="performSearch" 
        variant="primary"
        size="small"
        class="search-button" 
        aria-label="Search"
      >
        Search
      </AppButton>
    </div>
    
    <div class="filters" v-if="showFilters">
      <div class="filters-grid">
        <div class="filter-group">
          <label for="genre-filter">Genre</label>
          <select v-model="filters.genre" id="genre-filter" class="filter-select">
            <option value="">All Genres</option>
            <option v-for="genre in genres" :key="genre" :value="genre">{{ genre }}</option>
          </select>
        </div>
        
        <div class="filter-group">
          <label for="sort-by">Sort By</label>
          <select v-model="filters.sortBy" id="sort-by" class="filter-select">
            <option value="createdAt">Date Added</option>
            <option value="title">Title</option>
            <option value="views">Views</option>
          </select>
        </div>
        
        <div class="filter-group">
          <label for="sort-direction">Direction</label>
          <select v-model="filters.sortDirection" id="sort-direction" class="filter-select">
            <option value="DESC">Descending</option>
            <option value="ASC">Ascending</option>
          </select>
        </div>
      </div>
      
      <div class="filter-actions">
        <AppButton @click="applyFilters" variant="primary" size="small">
          Apply Filters
        </AppButton>
        <AppButton @click="clearFilters" variant="secondary" size="small">
          Reset
        </AppButton>
      </div>
    </div>
    
    <button class="toggle-filters" @click="showFilters = !showFilters">
      <BaseIcon :name="showFilters ? 'chevron-up' : 'chevron-down'" :size="12" />
      {{ showFilters ? 'Hide Filters' : 'Show Advanced Filters' }}
    </button>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useSearchStore } from '@/stores/search'
import BaseIcon from './icons/BaseIcon.vue'
import AppButton from './common/AppButton.vue'

const props = defineProps({
  initialQuery: {
    type: String,
    default: ''
  }
})

const emit = defineEmits(['search-performed'])

const searchStore = useSearchStore()
const searchQuery = ref(props.initialQuery)
const showFilters = ref(false)

// Default filters
const filters = ref({
  genre: '',
  sortBy: 'createdAt',
  sortDirection: 'DESC'
})

async function performSearch() {
  const searchParams = {
    query: searchQuery.value,
    genre: filters.value.genre,
    sortBy: filters.value.sortBy,
    sortDirection: filters.value.sortDirection,
    page: 0, // Reset to first page
    size: 20
  }
  
  try {
    await searchStore.searchVideos(searchParams)
    emit('search-performed', searchQuery.value)
  } catch (error) {
    console.error('Search failed:', error)
  }
}

function applyFilters() {
  performSearch()
}

function clearFilters() {
  filters.value = {
    genre: '',
    sortBy: 'createdAt',
    sortDirection: 'DESC'
  }
  performSearch()
}

function clearSearchInput() {
  searchQuery.value = ''
  performSearch()
}

onMounted(async () => {
  try {
    await searchStore.getGenres()
    
    // If there's an initial query, perform search
    if (props.initialQuery) {
      performSearch()
    }
  } catch (error) {
    console.error('Failed to load genres:', error)
  }
})
</script>

<style scoped>
.video-search-filter {
  margin-bottom: 1.5rem;
  padding: 1rem;
  background-color: var(--card-bg, #1e1e1e);
  border-radius: 8px;
  box-shadow: var(--shadow-md, 0 4px 6px rgba(0, 0, 0, 0.1));
}

.search-bar {
  display: flex;
  position: relative;
  margin-bottom: 1rem;
  align-items: center;
}

.search-icon {
  position: absolute;
  left: 10px;
  color: var(--text-muted, #888);
}

.search-input {
  flex: 1;
  padding: 0.5rem 1rem 0.5rem 2rem;
  border: 1px solid var(--input-border, #444444);
  border-radius: 4px;
  font-size: 0.95rem;
  background-color: var(--input-bg, #2a2a2a);
  color: var(--text-primary, #e0e0e0);
}

.clear-button {
  position: absolute;
  right: 80px;
}

.search-button {
  margin-left: 8px;
}

.filters {
  margin-top: 1rem;
  padding-top: 1rem;
  border-top: 1px solid var(--border-color, #333333);
}

.filters-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 1rem;
}

.filter-group {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.filter-group label {
  color: var(--text-secondary, #b0b0b0);
  font-size: 0.9rem;
}

.filter-select {
  padding: 0.5rem;
  border: 1px solid var(--input-border, #444444);
  border-radius: 4px;
  background-color: var(--input-bg, #2a2a2a);
  color: var(--text-primary, #e0e0e0);
  font-size: 0.9rem;
}

.filter-actions {
  margin-top: 1rem;
  display: flex;
  gap: 0.5rem;
  justify-content: flex-end;
}

.toggle-filters {
  background: none;
  border: none;
  color: var(--accent-primary, #3a86ff);
  cursor: pointer;
  font-size: 0.9rem;
  display: flex;
  align-items: center;
  gap: 0.25rem;
  margin-top: 0.5rem;
  padding: 0.25rem;
}

.toggle-filters:hover {
  text-decoration: underline;
}

@media (max-width: 640px) {
  .filters-grid {
    grid-template-columns: 1fr;
  }
  
  .filter-actions {
    justify-content: flex-start;
  }
}
</style>