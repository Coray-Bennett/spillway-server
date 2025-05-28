<template>
  <div class="videos-view">
    <h1>Videos</h1>
    
    <VideoSearchFilter @search-performed="handleSearchPerformed" />
    
    <div class="videos-actions">
      <div class="search-results-info" v-if="hasSearchResults">
        Found {{ totalResults }} videos
      </div>
      <router-link 
        v-if="isAuthenticated" 
        to="/upload" 
        class="upload-button"
      >
        <i class="fas fa-upload"></i> Upload Video
      </router-link>
    </div>
    
    <div v-if="error" class="error-message">
      {{ error }}
    </div>
    
    <div v-if="isLoading" class="loading-container">
      <div class="spinner"></div>
      <p>Loading videos...</p>
    </div>
    
    <div v-else-if="hasSearchResults" class="videos-grid">
      <VideoCard
        v-for="video in searchResults"
        :key="video.id"
        :video="video"
        @click.native="selectVideo(video)"
        class="video-card"
      />
    </div>
    
    <div v-else-if="recentVideos.length > 0" class="section">
      <h2>Recent Videos</h2>
      <div class="videos-grid">
        <VideoCard
          v-for="video in recentVideos"
          :key="video.id"
          :video="video"
          @click.native="selectVideo(video)"
          class="video-card"
        />
      </div>
    </div>
    
    <div v-else class="empty-state">
      <p>No videos found.</p>
      <router-link v-if="isAuthenticated" to="/upload" class="upload-button">
        Upload your first video
      </router-link>
    </div>
    
    <!-- Pagination controls -->
    <div v-if="totalPages > 1" class="pagination">
      <button 
        @click="changePage(currentPage - 1)" 
        :disabled="currentPage === 0"
        class="pagination-button"
      >
        Previous
      </button>
      
      <span class="page-info">
        Page {{ currentPage + 1 }} of {{ totalPages }}
      </span>
      
      <button 
        @click="changePage(currentPage + 1)" 
        :disabled="currentPage >= totalPages - 1"
        class="pagination-button"
      >
        Next
      </button>
    </div>
  </div>
</template>

<script>
import VideoCard from '../components/VideoCard.vue'
import VideoSearchFilter from '../components/VideoSearchFilter.vue'
import { useSearchStore } from '../stores/search'
import { useAuthStore } from '../stores/auth'
import { mapActions, mapState } from 'pinia'

export default {
  name: 'VideosView',
  
  components: {
    VideoCard,
    VideoSearchFilter
  },
  
  setup() {
    const searchStore = useSearchStore()
    const authStore = useAuthStore()
    return { searchStore, authStore }
  },
  
  data() {
    return {
      currentSearchQuery: ''
    }
  },
  
  computed: {
    ...mapState(useSearchStore, [
      'searchResults', 
      'recentVideos', 
      'isLoading', 
      'error', 
      'totalResults',
      'totalPages',
      'currentPage'
    ]),
    
    isAuthenticated() {
      return this.authStore.isAuthenticated
    },
    
    hasSearchResults() {
      return this.searchResults && this.searchResults.length > 0
    }
  },
  
  methods: {
    ...mapActions(useSearchStore, [
      'searchVideos', 
      'getRecentVideos',
      'clearSearch'
    ]),
    
    async handleSearchPerformed(query) {
      this.currentSearchQuery = query
    },
    
    selectVideo(video) {
      this.$router.push(`/video/${video.id}`)
    },
    
    async changePage(page) {
      if (page < 0 || page >= this.totalPages) return
      
      try {
        const searchParams = {
          query: this.currentSearchQuery,
          page,
          size: 20
        }
        
        await this.searchVideos(searchParams)
      } catch (error) {
        console.error('Failed to change page:', error)
      }
    }
  },
  
  async created() {
    try {
      // First try to load search results if there was a search
      const searchQuery = this.$route.query.q
      if (searchQuery) {
        this.currentSearchQuery = searchQuery
        await this.searchVideos({ query: searchQuery, page: 0, size: 20 })
      } else {
        // Otherwise load recent videos
        await this.getRecentVideos()
      }
    } catch (error) {
      console.error('Failed to load videos:', error)
    }
  },
  
  beforeDestroy() {
    this.clearSearch()
  }
}
</script>

<style scoped>
.videos-view {
  padding: 1.5rem;
}

h1 {
  margin-bottom: 1.5rem;
  color: #333;
}

.videos-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.5rem;
}

.upload-button {
  padding: 0.6rem 1.2rem;
  background-color: #0066cc;
  color: white;
  text-decoration: none;
  border-radius: 4px;
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.upload-button:hover {
  background-color: #0055aa;
}

.videos-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
  gap: 1.5rem;
  margin-bottom: 2rem;
}

.video-card {
  cursor: pointer;
}

.section h2 {
  margin: 1.5rem 0 1rem;
}

.error-message {
  background-color: #ffeeee;
  color: #cc0000;
  padding: 0.8rem;
  border-radius: 4px;
  margin-bottom: 1.5rem;
}

.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 3rem;
}

.spinner {
  width: 3rem;
  height: 3rem;
  border: 3px solid rgba(0, 102, 204, 0.3);
  border-radius: 50%;
  border-top-color: #0066cc;
  animation: spin 1s ease-in-out infinite;
  margin-bottom: 1rem;
}

.empty-state {
  text-align: center;
  padding: 3rem 1rem;
  background-color: #f9f9f9;
  border-radius: 8px;
}

.empty-state p {
  margin-bottom: 1.5rem;
  color: #666;
  font-size: 1.2rem;
}

.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  margin-top: 2rem;
  gap: 1rem;
}

.pagination-button {
  padding: 0.5rem 1rem;
  background-color: #f1f1f1;
  border: 1px solid #ddd;
  border-radius: 4px;
  cursor: pointer;
}

.pagination-button:hover:not(:disabled) {
  background-color: #e1e1e1;
}

.pagination-button:disabled {
  background-color: #f9f9f9;
  color: #ccc;
  cursor: not-allowed;
}

.page-info {
  color: #666;
}

.search-results-info {
  color: #666;
  font-size: 0.9rem;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}
</style>