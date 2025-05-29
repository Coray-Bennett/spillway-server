<template>
  <div class="videos-view">
    <div class="view-header">
      <h1>Videos</h1>
      
      <AppButton 
        v-if="isAuthenticated" 
        to="/upload" 
        icon="upload"
        variant="primary"
      >
        Upload Video
      </AppButton>
    </div>
    
    <VideoSearchFilter 
      :initial-query="currentSearchQuery"
      @search-performed="handleSearchPerformed" 
    />
    
    <div class="videos-actions" v-if="hasSearchResults">
      <div class="search-results-info">
        Found {{ totalResults }} videos
      </div>
    </div>
    
    <ErrorMessage v-if="error" :message="error" dismissible @dismiss="error = null" />
    
    <LoadingSpinner v-if="isLoading" message="Loading videos..." />
    
    <!-- Search results -->
    <div v-else-if="hasSearchResults" class="videos-grid">
      <VideoCard
        v-for="video in searchResults"
        :key="video.id"
        :video="video"
        @select="selectVideo"
      />
    </div>
    
    <!-- User's videos -->
    <div v-else-if="myVideos.length > 0" class="section">
      <h2>Your Videos</h2>
      <div class="videos-grid">
        <VideoCard
          v-for="video in myVideos"
          :key="video.id"
          :video="video"
          @select="selectVideo"
        />
      </div>
    </div>
    
    <!-- Recent videos -->
    <div v-else-if="recentVideos.length > 0" class="section">
      <h2>Recent Videos</h2>
      <div class="videos-grid">
        <VideoCard
          v-for="video in recentVideos"
          :key="video.id"
          :video="video"
          @select="selectVideo"
        />
      </div>
    </div>
    
    <!-- Empty state -->
    <div v-else class="empty-state">
      <BaseIcon name="video" :size="48" class="empty-icon" />
      <p>No videos found.</p>
      <AppButton v-if="isAuthenticated" to="/upload" variant="primary">
        Upload your first video
      </AppButton>
    </div>
    
    <!-- Pagination controls -->
    <div v-if="totalPages > 1" class="pagination">
      <AppButton 
        @click="changePage(currentPage - 1)" 
        :disabled="currentPage === 0"
        variant="secondary"
        size="small"
        icon="chevron-left"
      >
        Previous
      </AppButton>
      
      <span class="page-info">
        Page {{ currentPage + 1 }} of {{ totalPages }}
      </span>
      
      <AppButton 
        @click="changePage(currentPage + 1)" 
        :disabled="currentPage >= totalPages - 1"
        variant="secondary"
        size="small"
        icon="chevron-right"
        icon-position="right"
      >
        Next
      </AppButton>
    </div>
  </div>
</template>

<script setup>
import VideoCard from '@/components/VideoCard.vue'
import VideoSearchFilter from '@/components/VideoSearchFilter.vue'
import ErrorMessage from '@/components/common/ErrorMessage.vue'
import LoadingSpinner from '@/components/common/LoadingSpinner.vue'
import AppButton from '@/components/common/AppButton.vue'
import BaseIcon from '@/components/icons/BaseIcon.vue'
import useVideoList from '@/composables/useVideoList'

// Use the video list composable to handle all the logic
const {
  currentSearchQuery,
  myVideos,
  error,
  isLoading,
  currentPage,
  totalPages,
  totalResults,
  searchResults,
  recentVideos,
  isAuthenticated,
  hasSearchResults,
  handleSearchPerformed,
  selectVideo,
  changePage
} = useVideoList()
</script>

<style scoped>
.videos-view {
  width: 100%;
  max-width: 1200px;
  margin: 0 auto;
  padding: 1.5rem;
}

.view-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.5rem;
}

h1, h2 {
  color: var(--text-primary, #e0e0e0);
  margin: 0;
}

.videos-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.5rem;
}

.videos-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
  gap: 1.5rem;
  margin-bottom: 2rem;
}

.section h2 {
  margin: 1.5rem 0 1rem;
}

.empty-state {
  text-align: center;
  padding: 3rem 1rem;
  background-color: var(--card-bg, #2a2a2a);
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 1rem;
}

.empty-icon {
  opacity: 0.5;
  margin-bottom: 0.5rem;
}

.empty-state p {
  margin: 0 0 1rem;
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

.page-info {
  color: var(--text-secondary, #b0b0b0);
}

.search-results-info {
  color: var(--text-secondary, #b0b0b0);
  font-size: 0.9rem;
}

@media (max-width: 768px) {
  .view-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 1rem;
  }
  
  .videos-grid {
    grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  }
}
</style>