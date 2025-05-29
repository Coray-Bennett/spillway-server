import { ref, computed, onMounted, onBeforeUnmount, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useVideoStore } from '@/stores/video'
import { useSearchStore } from '@/stores/search'
import { useAuthStore } from '@/stores/auth'

/**
 * Composable for managing video lists with unified logic for loading,
 * searching, and pagination
 */
export default function useVideoList(options = {}) {
  const {
    loadMyVideosOnMount = true,
    loadRecentVideosOnMount = true,
    clearSearchOnUnmount = true,
    pageSize = 20
  } = options
  
  const route = useRoute()
  const router = useRouter()
  const videoStore = useVideoStore()
  const searchStore = useSearchStore()
  const authStore = useAuthStore()
  
  const currentSearchQuery = ref('')
  const myVideos = ref([])
  const error = ref(null)
  const isLoading = ref(false)
  
  // Handle pagination
  const currentPage = computed(() => searchStore.currentPage)
  const totalPages = computed(() => searchStore.totalPages)
  const totalResults = computed(() => searchStore.totalResults)
  const searchResults = computed(() => searchStore.searchResults)
  const recentVideos = computed(() => searchStore.recentVideos)
  const isAuthenticated = computed(() => authStore.isAuthenticated)
  
  const hasSearchResults = computed(() => 
    searchResults.value && searchResults.value.length > 0
  )
  
  // Methods
  async function loadMyVideos() {
    if (!isAuthenticated.value) return
    
    isLoading.value = true
    try {
      const result = await videoStore.getMyVideos()
      if (result && Array.isArray(result)) {
        myVideos.value = result
      }
    } catch (err) {
      error.value = err.message || 'Failed to load your videos'
    } finally {
      isLoading.value = false
    }
  }
  
  async function loadRecentVideos() {
    isLoading.value = true
    try {
      await searchStore.getRecentVideos()
    } catch (err) {
      error.value = err.message || 'Failed to load recent videos'
    } finally {
      isLoading.value = false
    }
  }
  
  async function handleSearchPerformed(query) {
    currentSearchQuery.value = query
  }
  
  function selectVideo(video) {
    router.push(`/video/${video.id}`)
  }
  
  async function changePage(page) {
    if (page < 0 || page >= totalPages.value) return
    
    try {
      const searchParams = {
        query: currentSearchQuery.value,
        page,
        size: pageSize
      }
      
      await searchStore.searchVideos(searchParams)
    } catch (err) {
      error.value = err.message || 'Failed to change page'
    }
  }
  
  // Lifecycle hooks
  onMounted(async () => {
    try {
      // Check for search query parameter
      const searchQuery = route.query.q
      if (searchQuery) {
        currentSearchQuery.value = searchQuery
        await searchStore.searchVideos({ 
          query: searchQuery, 
          page: 0, 
          size: pageSize 
        })
      } else {
        // Load user's videos first if authenticated
        if (loadMyVideosOnMount && isAuthenticated.value) {
          await loadMyVideos()
        }
        
        // If no videos found or not authenticated, load recent videos
        if (loadRecentVideosOnMount && (myVideos.value.length === 0 || !isAuthenticated.value)) {
          await loadRecentVideos()
        }
      }
    } catch (err) {
      error.value = err.message || 'Failed to load videos'
    }
  })
  
  onBeforeUnmount(() => {
    if (clearSearchOnUnmount) {
      searchStore.clearSearch()
    }
  })
  
  // Watchers
  watch(() => isAuthenticated.value, (newVal) => {
    if (newVal && myVideos.value.length === 0 && !currentSearchQuery.value) {
      // Re-load videos when authentication status changes
      loadMyVideos()
    }
  })
  
  return {
    // State
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
    
    // Methods
    loadMyVideos,
    loadRecentVideos,
    handleSearchPerformed,
    selectVideo,
    changePage
  }
}