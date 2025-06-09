<template>
  <div class="modal-overlay" @click.self="$emit('close')">
    <div class="modal-content">
      <div class="modal-header">
        <h3>Advanced Search</h3>
        <button @click="$emit('close')" class="close-btn">
          <BaseIcon name="x" :size="20" />
        </button>
      </div>

      <div class="modal-body">
        <!-- Search Type Toggle -->
        <div class="search-type-toggle">
          <button
            @click="searchType = 'videos'"
            :class="['toggle-btn', { active: searchType === 'videos' }]"
          >
            <BaseIcon name="video" :size="16" />
            Videos
          </button>
          <button
            @click="searchType = 'playlists'"
            :class="['toggle-btn', { active: searchType === 'playlists' }]"
          >
            <BaseIcon name="playlist" :size="16" />
            Playlists
          </button>
        </div>

        <!-- Video Search Form -->
        <form v-if="searchType === 'videos'" @submit.prevent="performVideoSearch" class="search-form">
          <div class="form-row">
            <div class="form-group">
              <label for="video-query">Search Query</label>
              <input
                id="video-query"
                v-model="videoSearchForm.query"
                type="text"
                class="form-input"
                placeholder="Enter search terms..."
              />
            </div>
          </div>

          <div class="form-row">
            <div class="form-group">
              <label for="genre">Genre</label>
              <select id="genre" v-model="videoSearchForm.genre" class="form-select">
                <option value="">All Genres</option>
                <option v-for="genre in genres" :key="genre" :value="genre">
                  {{ genre }}
                </option>
              </select>
            </div>

            <div class="form-group">
              <label for="sort-by">Sort By</label>
              <select id="sort-by" v-model="videoSearchForm.sortBy" class="form-select">
                <option value="">Default</option>
                <option value="title">Title</option>
                <option value="createdAt">Upload Date</option>
                <option value="fileSize">File Size</option>
                <option value="duration">Duration</option>
              </select>
            </div>

            <div class="form-group">
              <label for="sort-direction">Order</label>
              <select id="sort-direction" v-model="videoSearchForm.sortDirection" class="form-select">
                <option value="DESC">Descending</option>
                <option value="ASC">Ascending</option>
              </select>
            </div>
          </div>
        </form>

        <!-- Playlist Search Form -->
        <form v-if="searchType === 'playlists'" @submit.prevent="performPlaylistSearch" class="search-form">
          <div class="form-row">
            <div class="form-group">
              <label for="playlist-query">Search Query</label>
              <input
                id="playlist-query"
                v-model="playlistSearchForm.query"
                type="text"
                class="form-input"
                placeholder="Enter playlist name or description..."
              />
            </div>
          </div>

          <div class="form-row">
            <div class="form-group">
              <label for="playlist-sort-by">Sort By</label>
              <select id="playlist-sort-by" v-model="playlistSearchForm.sortBy" class="form-select">
                <option value="createdAt">Created Date</option>
                <option value="name">Name</option>
                <option value="videoCount">Video Count</option>
              </select>
            </div>

            <div class="form-group">
              <label for="playlist-sort-direction">Order</label>
              <select id="playlist-sort-direction" v-model="playlistSearchForm.sortDirection" class="form-select">
                <option value="DESC">Descending</option>
                <option value="ASC">Ascending</option>
              </select>
            </div>
          </div>
        </form>

        <!-- Search Actions -->
        <div class="search-actions">
          <button @click="clearSearch" type="button" class="btn btn-secondary">
            Clear
          </button>
          <button
            @click="performSearch"
            :disabled="searchStore.isLoading"
            class="btn btn-primary"
          >
            <BaseIcon name="search" :size="16" />
            <span v-if="searchStore.isLoading">Searching...</span>
            <span v-else>Search</span>
          </button>
        </div>

        <!-- Search Results -->
        <div v-if="hasResults" class="search-results">
          <div class="results-header">
            <h4>
              Search Results 
              <span class="result-count">
                ({{ searchStore.totalResults }} total)
              </span>
            </h4>
          </div>

          <!-- Video Results -->
          <div v-if="searchType === 'videos' && videoResults.length > 0" class="results-grid">
            <div
              v-for="video in videoResults"
              :key="video.id"
              class="result-item video-item"
            >
              <div class="item-thumbnail">
                <BaseIcon name="video" :size="24" />
              </div>
              <div class="item-content">
                <h5 class="item-title">{{ video.title }}</h5>
                <p class="item-description">{{ video.description }}</p>
                <div class="item-meta">
                  <span class="genre">{{ video.genre }}</span>
                  <span class="upload-date">{{ formatDate(video.createdAt) }}</span>
                </div>
              </div>
              <div class="item-actions">
                <router-link :to="`/video/${video.id}`" class="btn btn-sm btn-primary">
                  Watch
                </router-link>
              </div>
            </div>
          </div>

          <!-- Playlist Results -->
          <div v-if="searchType === 'playlists' && playlistResults.length > 0" class="results-grid">
            <div
              v-for="playlist in playlistResults"
              :key="playlist.id"
              class="result-item playlist-item"
            >
              <div class="item-thumbnail">
                <BaseIcon name="playlist" :size="24" />
              </div>
              <div class="item-content">
                <h5 class="item-title">{{ playlist.name }}</h5>
                <p class="item-description">{{ playlist.description }}</p>
                <div class="item-meta">
                  <span class="video-count">{{ playlist.videoCount || 0 }} videos</span>
                  <span class="created-date">{{ formatDate(playlist.createdAt) }}</span>
                </div>
              </div>
              <div class="item-actions">
                <router-link :to="`/playlists/${playlist.id}`" class="btn btn-sm btn-primary">
                  View
                </router-link>
              </div>
            </div>
          </div>

          <!-- Pagination -->
          <div v-if="searchStore.totalPages > 1" class="pagination">
            <button
              @click="goToPage(searchStore.currentPage - 1)"
              :disabled="searchStore.currentPage === 0"
              class="btn btn-outline btn-sm"
            >
              Previous
            </button>
            
            <span class="page-info">
              Page {{ searchStore.currentPage + 1 }} of {{ searchStore.totalPages }}
            </span>
            
            <button
              @click="goToPage(searchStore.currentPage + 1)"
              :disabled="searchStore.currentPage >= searchStore.totalPages - 1"
              class="btn btn-outline btn-sm"
            >
              Next
            </button>
          </div>
        </div>

        <!-- Error Message -->
        <div v-if="searchStore.error" class="error-message">
          {{ searchStore.error }}
        </div>

        <!-- Empty Results -->
        <div v-if="searchPerformed && !hasResults && !searchStore.isLoading && !searchStore.error" class="empty-results">
          <BaseIcon name="search" :size="48" />
          <h4>No Results Found</h4>
          <p>Try adjusting your search criteria or search terms.</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useSearchStore } from '@/stores/search'
import BaseIcon from './icons/BaseIcon.vue'
import { formatDate } from '@/utils/date'

const emit = defineEmits(['close', 'result-selected'])

const searchStore = useSearchStore()
const searchType = ref('videos')
const searchPerformed = ref(false)
const genres = ref([])

const videoSearchForm = reactive({
  query: '',
  genre: '',
  sortBy: '',
  sortDirection: 'DESC',
  page: 0,
  size: 20
})

const playlistSearchForm = reactive({
  query: '',
  sortBy: 'createdAt',
  sortDirection: 'DESC',
  page: 0,
  size: 20
})

const videoResults = computed(() => searchStore.searchResults)
const playlistResults = computed(() => searchStore.playlists)

const hasResults = computed(() => {
  if (searchType.value === 'videos') {
    return videoResults.value.length > 0
  } else {
    return playlistResults.value.length > 0
  }
})

onMounted(async () => {
  // Load available genres
  const result = await searchStore.getGenres()
  if (result.success) {
    genres.value = result.genres
  }
})

async function performSearch() {
  searchPerformed.value = true
  
  if (searchType.value === 'videos') {
    await performVideoSearch()
  } else {
    await performPlaylistSearch()
  }
}

async function performVideoSearch() {
  const searchParams = {
    ...videoSearchForm
  }
  
  const result = await searchStore.searchVideos(searchParams)
  
  if (result.success) {
    console.log('Video search completed successfully')
  }
}

async function performPlaylistSearch() {
  const searchParams = {
    ...playlistSearchForm
  }
  
  const result = await searchStore.searchPlaylists(searchParams)
  
  if (result.success) {
    console.log('Playlist search completed successfully')
  }
}

async function goToPage(page) {
  if (searchType.value === 'videos') {
    videoSearchForm.page = page
    await performVideoSearch()
  } else {
    playlistSearchForm.page = page
    await performPlaylistSearch()
  }
}

function clearSearch() {
  if (searchType.value === 'videos') {
    Object.assign(videoSearchForm, {
      query: '',
      genre: '',
      sortBy: '',
      sortDirection: 'DESC',
      page: 0,
      size: 20
    })
  } else {
    Object.assign(playlistSearchForm, {
      query: '',
      sortBy: 'createdAt',
      sortDirection: 'DESC',
      page: 0,
      size: 20
    })
  }
  
  searchStore.clearSearch()
  searchPerformed.value = false
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
  max-width: 800px;
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

.search-type-toggle {
  display: flex;
  gap: 0.5rem;
  margin-bottom: 1.5rem;
  padding: 0.25rem;
  background-color: var(--primary-bg);
  border-radius: 0.5rem;
}

.toggle-btn {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  padding: 0.75rem;
  border: none;
  background: none;
  color: var(--secondary-text);
  border-radius: 0.375rem;
  cursor: pointer;
  transition: var(--transition);
  font-weight: 500;
}

.toggle-btn.active {
  background-color: var(--accent-color);
  color: white;
}

.search-form {
  margin-bottom: 1.5rem;
}

.form-row {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 1rem;
  margin-bottom: 1rem;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.form-group label {
  font-weight: 500;
  color: var(--primary-text);
  font-size: 0.875rem;
}

.form-input, .form-select {
  padding: 0.75rem;
  border: 1px solid var(--border-color);
  border-radius: 0.5rem;
  background-color: var(--primary-bg);
  color: var(--primary-text);
  transition: var(--transition);
}

.form-input:focus, .form-select:focus {
  outline: none;
  border-color: var(--accent-color);
  box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.1);
}

.search-actions {
  display: flex;
  gap: 1rem;
  justify-content: flex-end;
  margin-bottom: 1.5rem;
}

.search-results {
  border-top: 1px solid var(--border-color);
  padding-top: 1.5rem;
}

.results-header {
  margin-bottom: 1rem;
}

.results-header h4 {
  margin: 0;
  color: var(--primary-text);
}

.result-count {
  color: var(--secondary-text);
  font-weight: normal;
  font-size: 0.875rem;
}

.results-grid {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  margin-bottom: 1.5rem;
}

.result-item {
  display: flex;
  align-items: flex-start;
  gap: 1rem;
  padding: 1rem;
  background-color: var(--primary-bg);
  border-radius: 0.5rem;
  border: 1px solid var(--border-color);
  transition: var(--transition);
}

.result-item:hover {
  transform: translateY(-1px);
  box-shadow: 0 2px 4px -1px rgba(0, 0, 0, 0.1);
}

.item-thumbnail {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  background-color: var(--secondary-bg);
  border-radius: 0.5rem;
  color: var(--accent-color);
  flex-shrink: 0;
}

.item-content {
  flex: 1;
  min-width: 0;
}

.item-title {
  margin: 0 0 0.5rem 0;
  color: var(--primary-text);
  font-size: 1rem;
  font-weight: 600;
}

.item-description {
  margin: 0 0 0.5rem 0;
  color: var(--secondary-text);
  font-size: 0.875rem;
  line-height: 1.4;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.item-meta {
  display: flex;
  gap: 1rem;
  font-size: 0.75rem;
  color: var(--secondary-text);
}

.genre {
  background-color: var(--accent-color);
  color: white;
  padding: 0.125rem 0.5rem;
  border-radius: 0.25rem;
  font-weight: 500;
}

.item-actions {
  flex-shrink: 0;
}

.pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 1rem;
}

.page-info {
  color: var(--secondary-text);
  font-size: 0.875rem;
}

.error-message {
  background-color: rgba(239, 68, 68, 0.1);
  border: 1px solid rgba(239, 68, 68, 0.3);
  color: #ef4444;
  padding: 0.75rem;
  border-radius: 0.5rem;
}

.empty-results {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 1rem;
  padding: 2rem;
  text-align: center;
  color: var(--secondary-text);
}

@media (max-width: 768px) {
  .modal-overlay {
    padding: 0.5rem;
  }
  
  .form-row {
    grid-template-columns: 1fr;
  }
  
  .search-actions {
    justify-content: stretch;
  }
  
  .search-actions .btn {
    flex: 1;
  }
  
  .result-item {
    flex-direction: column;
    align-items: stretch;
  }
  
  .item-meta {
    flex-direction: column;
    gap: 0.5rem;
  }
  
  .pagination {
    flex-direction: column;
    gap: 0.5rem;
  }
}
</style>