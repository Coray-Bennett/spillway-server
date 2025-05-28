<template>
  <div class="video-search-filter">
    <div class="search-bar">
      <input
        v-model="searchQuery"
        type="text"
        placeholder="Search videos..."
        @keyup.enter="performSearch"
        class="search-input"
      />
      <button @click="performSearch" class="search-button" aria-label="Search">
        <i class="fas fa-search"></i>
      </button>
    </div>
    
    <div class="filters" v-if="showFilters">
      <div class="filter-group">
        <label for="genre-filter">Genre:</label>
        <select v-model="filters.genre" id="genre-filter">
          <option value="">All Genres</option>
          <option v-for="genre in genres" :key="genre" :value="genre">{{ genre }}</option>
        </select>
      </div>
      
      <div class="filter-group">
        <label for="sort-by">Sort By:</label>
        <select v-model="filters.sortBy" id="sort-by">
          <option value="createdAt">Date Added</option>
          <option value="title">Title</option>
          <option value="views">Views</option>
        </select>
        <select v-model="filters.sortDirection" id="sort-direction">
          <option value="DESC">Descending</option>
          <option value="ASC">Ascending</option>
        </select>
      </div>
      
      <div class="filter-actions">
        <button @click="applyFilters" class="filter-button">Apply Filters</button>
        <button @click="clearFilters" class="reset-button">Reset</button>
      </div>
    </div>
    
    <div class="toggle-filters" @click="showFilters = !showFilters">
      {{ showFilters ? 'Hide Filters' : 'Show Advanced Filters' }}
    </div>
  </div>
</template>

<script>
import { useSearchStore } from '../stores/search'
import { mapActions, mapState } from 'pinia'

export default {
  name: 'VideoSearchFilter',
  
  data() {
    return {
      searchQuery: '',
      showFilters: false,
      filters: {
        genre: '',
        sortBy: 'createdAt',
        sortDirection: 'DESC'
      }
    }
  },
  
  computed: {
    ...mapState(useSearchStore, ['genres'])
  },
  
  methods: {
    ...mapActions(useSearchStore, ['searchVideos', 'getGenres']),
    
    async performSearch() {
      const searchParams = {
        query: this.searchQuery,
        genre: this.filters.genre,
        sortBy: this.filters.sortBy,
        sortDirection: this.filters.sortDirection,
        page: 0, // Reset to first page
        size: 20
      }
      
      try {
        await this.searchVideos(searchParams)
        this.$emit('search-performed', this.searchQuery)
      } catch (error) {
        console.error('Search failed:', error)
      }
    },
    
    applyFilters() {
      this.performSearch()
    },
    
    clearFilters() {
      this.filters = {
        genre: '',
        sortBy: 'createdAt',
        sortDirection: 'DESC'
      }
      this.performSearch()
    }
  },
  
  async mounted() {
    // Load genres for dropdown
    try {
      await this.getGenres()
    } catch (error) {
      console.error('Failed to load genres:', error)
    }
  }
}
</script>

<style scoped>
.video-search-filter {
  margin-bottom: 1.5rem;
  padding: 1rem;
  background-color: #f9f9f9;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
}

.search-bar {
  display: flex;
  margin-bottom: 1rem;
}

.search-input {
  flex: 1;
  padding: 0.5rem 1rem;
  border: 1px solid #ddd;
  border-radius: 4px 0 0 4px;
  font-size: 1rem;
}

.search-button {
  background-color: #0066cc;
  color: white;
  border: none;
  padding: 0 1rem;
  border-radius: 0 4px 4px 0;
  cursor: pointer;
}

.search-button:hover {
  background-color: #0055aa;
}

.filters {
  margin-top: 1rem;
  padding-top: 1rem;
  border-top: 1px solid #ddd;
}

.filter-group {
  margin-bottom: 1rem;
  display: flex;
  align-items: center;
}

.filter-group label {
  margin-right: 0.5rem;
  min-width: 60px;
}

.filter-group select {
  padding: 0.3rem;
  margin-right: 0.5rem;
  border: 1px solid #ddd;
  border-radius: 4px;
}

.filter-actions {
  margin-top: 1rem;
  display: flex;
  gap: 0.5rem;
}

.filter-button, .reset-button {
  padding: 0.5rem 1rem;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 0.9rem;
}

.filter-button {
  background-color: #0066cc;
  color: white;
}

.filter-button:hover {
  background-color: #0055aa;
}

.reset-button {
  background-color: #f1f1f1;
  color: #333;
}

.reset-button:hover {
  background-color: #e1e1e1;
}

.toggle-filters {
  text-align: center;
  color: #0066cc;
  cursor: pointer;
  font-size: 0.9rem;
  margin-top: 1rem;
}

.toggle-filters:hover {
  text-decoration: underline;
}
</style>