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
    
    <!-- Search results -->
    <VideoGallery
      v-if="hasSearchResults"
      :videos="searchResults"
      :loading="isLoading"
      :error="error"
      :current-page="currentPage"
      :total-pages="totalPages"
      title="Search Results"
      :count="totalResults"
      layout="grid"
      show-pagination
      @select="selectVideo"
      @page-change="changePage"
    >
      <template #actions>
        <AppButton 
          v-if="currentSearchQuery" 
          @click="clearSearch" 
          variant="text"
          size="small"
          icon="close"
        >
          Clear Search
        </AppButton>
      </template>
    </VideoGallery>
    
    <!-- User's videos section -->
    <VideoGallery
      v-else-if="myVideosLoading || myVideos.length > 0"
      :videos="myVideos"
      :loading="myVideosLoading"
      :error="myVideosError"
      title="Your Videos"
      layout="grid"
      show-video-actions
      @select="selectVideo"
      @play="playVideo"
    >
      
      <template #empty-actions>
        <AppButton to="/upload" variant="primary" icon="upload">
          Upload your first video
        </AppButton>
      </template>
    </VideoGallery>
    
    <!-- If absolutely no videos found -->
    <EmptyState 
      v-if="!isLoading && !hasSearchResults && myVideos.length === 0 && recentVideos.length === 0"
      icon="video"
      title="No videos found"
      description="There are currently no videos available to display."
    >
      <template #actions>
        <AppButton v-if="isAuthenticated" to="/upload" variant="primary">
          Upload your first video
        </AppButton>
      </template>
    </EmptyState>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import VideoGallery from '@/components/VideoGallery.vue'
import VideoSearchFilter from '@/components/VideoSearchFilter.vue'
import AppButton from '@/components/common/AppButton.vue'
import EmptyState from '@/components/common/EmptyState.vue'
import { useVideoStore } from '@/stores/video'
import { useSearchStore } from '@/stores/search'
import { useAuthStore } from '@/stores/auth'
import { useRouter } from 'vue-router'

// Stores
const videoStore = useVideoStore()
const searchStore = useSearchStore()
const authStore = useAuthStore()
const router = useRouter()

// State
const currentSearchQuery = ref('')
const error = ref(null)
const isLoading = ref(false)

// My videos section state
const myVideos = ref([])
const myVideosLoading = ref(false)
const myVideosError = ref(null)

// Recent videos section state
const recentVideosLoading = ref(false)
const recentVideosError = ref(null)

// Computed properties
const isAuthenticated = computed(() => authStore.isAuthenticated)
const searchResults = computed(() => searchStore.searchResults || [])
const totalResults = computed(() => searchStore.totalResults)
const totalPages = computed(() => searchStore.totalPages)
const currentPage = computed(() => searchStore.currentPage)
const recentVideos = computed(() => searchStore.recentVideos || [])
const hasSearchResults = computed(() => searchResults.value.length > 0)

// Methods
async function loadMyVideos() {
  if (!isAuthenticated.value) return
  
  myVideosLoading.value = true
  myVideosError.value = null
  
  try {
    const result = await videoStore.getMyVideos()
    if (Array.isArray(result)) {
      myVideos.value = result
    } else if (result.error) {
      myVideosError.value = result.error
    }
  } catch (err) {
    myVideosError.value = err.message || 'Failed to load your videos'
  } finally {
    myVideosLoading.value = false
  }
}

async function loadRecentVideos() {
  recentVideosLoading.value = true
  recentVideosError.value = null
  
  try {
    await searchStore.getRecentVideos()
  } catch (err) {
    recentVideosError.value = err.message || 'Failed to load recent videos'
  } finally {
    recentVideosLoading.value = false
  }
}

async function handleSearchPerformed(query) {
  currentSearchQuery.value = query
  await performSearch(query)
}

async function performSearch(query) {
  isLoading.value = true
  error.value = null
  
  try {
    await searchStore.searchVideos({
      query,
      page: 0,
      size: 20
    })
  } catch (err) {
    error.value = err.message || 'Search failed'
  } finally {
    isLoading.value = false
  }
}

function selectVideo(video) {
  router.push(`/video/${video.id}`)
}

function playVideo(video) {
  router.push(`/video/${video.id}?autoplay=1`)
}

async function changePage(page) {
  isLoading.value = true
  error.value = null
  
  try {
    await searchStore.searchVideos({
      query: currentSearchQuery.value,
      page,
      size: 20
    })
  } catch (err) {
    error.value = err.message || 'Failed to change page'
  } finally {
    isLoading.value = false
  }
}

function clearSearch() {
  currentSearchQuery.value = ''
  searchStore.clearSearch()
}

// Lifecycle hooks
onMounted(async () => {
  // Ensure auth is initialized
  if (!authStore.user && authStore.token) {
    authStore.initializeAuth()
  }
  
  // Check for search query from URL
  const urlParams = new URLSearchParams(window.location.search)
  const queryParam = urlParams.get('q')
  
  if (queryParam) {
    currentSearchQuery.value = queryParam
    await performSearch(queryParam)
  } else {
    // If authenticated, load user's videos
    if (isAuthenticated.value) {
      loadMyVideos()
    }
    
    // Always load recent videos as fallback content
    loadRecentVideos()
  }
})
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

h1 {
  color: var(--text-primary, #e0e0e0);
  margin: 0;
}

@media (max-width: 768px) {
  .view-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 1rem;
  }
}
</style>