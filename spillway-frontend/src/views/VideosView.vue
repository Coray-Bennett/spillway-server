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
    
    <div v-else-if="myVideos.length > 0" class="section">
      <h2>Your Videos</h2>
      <div class="videos-grid">
        <VideoCard
          v-for="video in myVideos"
          :key="video.id"
          :video="video"
          @click.native="selectVideo(video)"
          class="video-card"
        />
      </div>
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
import { useVideoStore } from '../stores/video' 
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
    const videoStore = useVideoStore()
    const authStore = useAuthStore()
    return { searchStore, videoStore, authStore }
  },
  
  data() {
    return {
      currentSearchQuery: '',
      myVideos: []
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
    
    async loadMyVideos() {
      this.isLoading = true
      try {
        const result = await this.videoStore.getMyVideos()
        if (result && Array.isArray(result)) {
          this.myVideos = result
        }
      } catch (error) {
        console.error('Failed to load my videos:', error)
        if (error.response && error.response.status === 401) {
          this.error = 'Please log in to view your videos'
        } else {
          this.error = error.message || 'Failed to load videos'
        }
      } finally {
        this.isLoading = false
      }
    },
    
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
        // If authenticated, load user's videos first
        if (this.isAuthenticated) {
          await this.loadMyVideos()
        }
        
        // If no videos found or not authenticated, load recent videos as fallback
        if (this.myVideos.length === 0) {
          await this.getRecentVideos()
        }
      }
    } catch (error) {
      console.error('Failed to load videos:', error)
    }
  },
  
  beforeDestroy() {
    this.clearSearch()
  },
  
  watch: {
    isAuthenticated(newVal) {
      if (newVal && this.myVideos.length === 0 && !this.currentSearchQuery) {
        // Re-load videos when authentication status changes
        this.loadMyVideos()
      }
    }
  }
}
</script>

<style scoped>
/* Dark mode styles */
.videos-view {
  padding: 1.5rem;
  background-color: var(--bg-primary, #1a1a1a);
  color: var(--text-primary, #e0e0e0);
  min-height: 100vh;
}

h1, h2 {
  color: var(--text-primary, #e0e0e0);
}

.videos-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.5rem;
}

.upload-button {
  padding: 0.6rem 1.2rem;
  background-color: var(--accent-primary, #3a86ff);
  color: white;
  text-decoration: none;
  border-radius: 4px;
  display: flex;
  align-items: center;
  gap: 0.5rem;
  border: none;
}

.upload-button:hover {
  background-color: var(--button-primary-hover, #2a76ef);
  text-decoration: none;
}

.videos-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
  gap: 1.5rem;
  margin-bottom: 2rem;
}

.video-card {
  cursor: pointer;
  background-color: var(--card-bg, #2a2a2a);
  border-radius: 8px;
  overflow: hidden;
  transition: transform 0.2s, box-shadow 0.2s;
}

.video-card:hover {
  transform: translateY(-5px);
  box-shadow: var(--shadow-lg, 0 10px 15px rgba(0, 0, 0, 0.2));
}

.section h2 {
  margin: 1.5rem 0 1rem;
  color: var(--text-primary, #e0e0e0);
}

.error-message {
  background-color: rgba(251, 86, 7, 0.1);
  color: var(--accent-danger, #fb5607);
  padding: 0.8rem;
  border-radius: 4px;
  margin-bottom: 1.5rem;
  border: 1px solid rgba(251, 86, 7, 0.3);
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
  border: 3px solid rgba(162, 162, 162, 0.3);
  border-radius: 50%;
  border-top-color: var(--accent-primary, #3a86ff);
  animation: spin 1s ease-in-out infinite;
  margin-bottom: 1rem;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.empty-state {
  text-align: center;
  padding: 3rem 1rem;
  background-color: var(--card-bg, #2a2a2a);
  border-radius: 8px;
}

.empty-state p {
  margin-bottom: 1.5rem;
  color: var(--text-secondary, #b0b0b0);
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
  background-color: var(--button-secondary-bg, #2a2a2a);
  border: 1px solid var(--border-color, #333333);
  border-radius: 4px;
  cursor: pointer;
  color: var(--text-primary, #e0e0e0);
}

.pagination-button:hover:not(:disabled) {
  background-color: var(--bg-tertiary, #333333);
}

.pagination-button:disabled {
  background-color: var(--bg-tertiary, #2a2a2a);
  color: var(--text-muted, #888888);
  cursor: not-allowed;
}

.page-info {
  color: var(--text-secondary, #b0b0b0);
}

.search-results-info {
  color: var(--text-secondary, #b0b0b0);
  font-size: 0.9rem;
}

/* Import dark mode for video search filter component */
@import url('./VideoSearchFilter.vue');
</style>