import { defineStore } from 'pinia'
import { searchAPI } from '@/services/apiService'

export const useSearchStore = defineStore('search', {
  state: () => ({
    videos: [],
    playlists: [],
    genres: [],
    recentVideos: [],
    popularPlaylists: [],
    searchResults: [],
    totalResults: 0,
    totalPages: 0,
    currentPage: 0,
    pageSize: 20,
    isLoading: false,
    error: null,
    lastSearchParams: null
  }),
  
  actions: {
    // Error handling helper
    handleError(error, defaultMessage) {
      console.error('[Search Store]', defaultMessage, error)
      
      if (error.response && error.response.data) {
        if (typeof error.response.data === 'object' && error.response.data.message) {
          this.error = error.response.data.message
        } else {
          this.error = error.response.data
        }
      } else if (error.message) {
        this.error = error.message
      } else {
        this.error = defaultMessage
      }
      
      return { success: false, error: this.error }
    },

    async searchVideos(searchParams = {}) {
      this.isLoading = true
      this.error = null
      
      // Store the search params for potential retry
      this.lastSearchParams = { ...searchParams }
      
      try {
        // Convert flat parameters to proper request format
        const request = {
          query: searchParams.query || '',
          genre: searchParams.genre || '',
          page: searchParams.page !== undefined ? searchParams.page : 0,
          size: searchParams.size || 20,
          sortBy: searchParams.sortBy || '',
          sortDirection: searchParams.sortDirection || 'DESC'
        }
        
        console.log('[Search Store] Searching videos with params:', request)
        const response = await searchAPI.searchVideos(request)
        const { content, totalElements, totalPages, number, size } = response.data
        
        this.searchResults = content
        this.totalResults = totalElements
        this.totalPages = totalPages
        this.currentPage = number
        this.pageSize = size
        
        console.log(`[Search Store] Found ${totalElements} videos across ${totalPages} pages`)
        
        return {
          success: true,
          videos: content,
          pagination: {
            totalResults: totalElements,
            totalPages,
            currentPage: number,
            pageSize: size
          }
        }
      } catch (error) {
        return this.handleError(error, 'Failed to search videos')
      } finally {
        this.isLoading = false
      }
    },
    
    async searchPlaylists(searchParams = {}) {
      this.isLoading = true
      this.error = null
      
      try {
        const request = {
          query: searchParams.query || '',
          page: searchParams.page || 0,
          size: searchParams.size || 20,
          sortBy: searchParams.sortBy || 'createdAt',
          sortDirection: searchParams.sortDirection || 'DESC'
        }
        
        console.log('[Search Store] Searching playlists with params:', request)
        const response = await searchAPI.searchPlaylists(request)
        const { content, totalElements, totalPages, number, size } = response.data
        
        this.playlists = content
        this.totalResults = totalElements
        this.totalPages = totalPages
        this.currentPage = number
        this.pageSize = size
        
        return {
          success: true,
          playlists: content,
          pagination: {
            totalResults: totalElements,
            totalPages,
            currentPage: number,
            pageSize: size
          }
        }
      } catch (error) {
        return this.handleError(error, 'Failed to search playlists')
      } finally {
        this.isLoading = false
      }
    },
    
    async getGenres() {
      try {
        console.log('[Search Store] Fetching genres')
        const response = await searchAPI.getGenres()
        this.genres = response.data
        return { success: true, genres: response.data }
      } catch (error) {
        return this.handleError(error, 'Failed to fetch genres')
      }
    },
    
    async getRecentVideos(limit = 10) {
      this.isLoading = true
      this.error = null
      
      try {
        console.log('[Search Store] Fetching recent videos, limit:', limit)
        const response = await searchAPI.getRecentVideos(limit)
        this.recentVideos = response.data
        return { success: true, videos: response.data }
      } catch (error) {
        return this.handleError(error, 'Failed to fetch recent videos')
      } finally {
        this.isLoading = false
      }
    },
    
    async getPopularPlaylists(limit = 10) {
      this.isLoading = true
      this.error = null
      
      try {
        console.log('[Search Store] Fetching popular playlists, limit:', limit)
        const response = await searchAPI.getPopularPlaylists(limit)
        this.popularPlaylists = response.data
        return { success: true, playlists: response.data }
      } catch (error) {
        return this.handleError(error, 'Failed to fetch popular playlists')
      } finally {
        this.isLoading = false
      }
    },
    
    async quickSearch(query, page = 0, size = 20) {
      this.isLoading = true
      this.error = null
      
      try {
        console.log(`[Search Store] Quick searching: "${query}", page: ${page}`)
        const response = await searchAPI.quickSearch(query, page, size)
        
        const { content, totalElements, totalPages, number, size: pageSize } = response.data
        
        this.searchResults = content
        this.totalResults = totalElements
        this.totalPages = totalPages
        this.currentPage = number
        this.pageSize = pageSize
        
        console.log(`[Search Store] Quick search found ${totalElements} results`)
        
        return {
          success: true,
          videos: content,
          pagination: {
            totalResults: totalElements,
            totalPages,
            currentPage: number,
            pageSize
          }
        }
      } catch (error) {
        return this.handleError(error, 'Failed to perform quick search')
      } finally {
        this.isLoading = false
      }
    },
    
    // Retry last search with same parameters
    async retryLastSearch() {
      if (this.lastSearchParams) {
        return await this.searchVideos(this.lastSearchParams)
      }
      return { success: false, error: 'No previous search to retry' }
    },
    
    clearSearch() {
      console.log('[Search Store] Clearing search results')
      this.searchResults = []
      this.totalResults = 0
      this.totalPages = 0
      this.currentPage = 0
      this.error = null
    },
    
    clearError() {
      this.error = null
    }
  }
})