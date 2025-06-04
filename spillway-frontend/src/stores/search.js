import { defineStore } from 'pinia'
import axios from 'axios'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8081'

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
    error: null
  }),
  
  actions: {
    async searchVideos(searchParams = {}) {
      this.isLoading = true
      this.error = null
      
      try {
        // Convert flat parameters to proper request format
        const request = {
          query: searchParams.query || '',
          genre: searchParams.genre || '',
          page: searchParams.page || 0,
          size: searchParams.size || 20,
          sortBy: searchParams.sortBy || '',
          sortDirection: searchParams.sortDirection || 'DESC'
        }
        
        const response = await axios.post(`${API_BASE_URL}/search/videos`, request)
        const { content, totalElements, totalPages, number, size } = response.data
        
        this.searchResults = content
        this.totalResults = totalElements
        this.totalPages = totalPages
        this.currentPage = number
        this.pageSize = size
        
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
        this.error = error.response?.data || 'Failed to search videos'
        return { success: false, error: this.error }
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
        
        const response = await axios.post(`${API_BASE_URL}/search/playlists`, request)
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
        this.error = error.response?.data || 'Failed to search playlists'
        return { success: false, error: this.error }
      } finally {
        this.isLoading = false
      }
    },
    
    async getGenres() {
      try {
        const response = await axios.get(`${API_BASE_URL}/search/genres`)
        this.genres = response.data
        return { success: true, genres: response.data }
      } catch (error) {
        this.error = error.response?.data || 'Failed to fetch genres'
        return { success: false, error: this.error }
      }
    },
    
    async getRecentVideos(limit = 10) {
      this.isLoading = true
      this.error = null
      
      try {
        const response = await axios.get(`${API_BASE_URL}/search/videos/recent?limit=${limit}`)
        this.recentVideos = response.data
        return { success: true, videos: response.data }
      } catch (error) {
        this.error = error.response?.data || 'Failed to fetch recent videos'
        return { success: false, error: this.error }
      } finally {
        this.isLoading = false
      }
    },
    
    async getPopularPlaylists(limit = 10) {
      this.isLoading = true
      this.error = null
      
      try {
        const response = await axios.get(`${API_BASE_URL}/search/playlists/popular?limit=${limit}`)
        this.popularPlaylists = response.data
        return { success: true, playlists: response.data }
      } catch (error) {
        this.error = error.response?.data || 'Failed to fetch popular playlists'
        return { success: false, error: this.error }
      } finally {
        this.isLoading = false
      }
    },
    
    async quickSearch(query, page = 0, size = 20) {
      this.isLoading = true
      this.error = null
      
      try {
        const response = await axios.get(
          `${API_BASE_URL}/search/videos/quick?q=${encodeURIComponent(query)}&page=${page}&size=${size}`
        )
        
        const { content, totalElements, totalPages, number, size: pageSize } = response.data
        
        this.searchResults = content
        this.totalResults = totalElements
        this.totalPages = totalPages
        this.currentPage = number
        this.pageSize = pageSize
        
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
        this.error = error.response?.data || 'Failed to perform quick search'
        return { success: false, error: this.error }
      } finally {
        this.isLoading = false
      }
    },
    
    clearSearch() {
      this.searchResults = []
      this.totalResults = 0
      this.totalPages = 0
      this.currentPage = 0
      this.error = null
    }
  }
})