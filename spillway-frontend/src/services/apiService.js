import axios from 'axios'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8081'

/**
 * Create axios instance with default config
 */
const apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json'
  }
})

/**
 * Setup interceptors for authentication and error handling
 */
const setupInterceptors = () => {
  // Request interceptor - adds the auth token to every request
  apiClient.interceptors.request.use(
    config => {
      const token = localStorage.getItem('token')
      if (token) {
        config.headers.Authorization = `Bearer ${token}`
      }
      return config
    },
    error => Promise.reject(error)
  )

  // Response interceptor - handle common errors
  apiClient.interceptors.response.use(
    response => response,
    error => {
      // Global error handling
      if (error.response) {
        // Handle 401 errors
        if (error.response.status === 401) {
          // Handle unauthorized - we may want to clear auth or redirect
          console.warn('Unauthorized API request')
        }
        
        // Handle 403 errors
        if (error.response.status === 403) {
          console.warn('Forbidden API request')
        }
        
        // Log server errors
        if (error.response.status >= 500) {
          console.error('Server error:', error.response.status)
        }
      } else if (error.request) {
        // Request was made but no response received
        console.error('Network error - no response received')
      } else {
        // Request setup error
        console.error('Request error:', error.message)
      }
      
      return Promise.reject(error)
    }
  )
}

// Initialize interceptors
setupInterceptors()

/**
 * Auth API service
 */
export const authAPI = {
  login: (credentials) => apiClient.post('/auth/login', credentials),
  register: (userData) => apiClient.post('/auth/register', userData),
  confirmEmail: (token) => apiClient.get(`/auth/confirm?token=${token}`),
  resendConfirmation: (email) => apiClient.post('/auth/resend-confirmation', { email }),
  
  /**
   * Update authentication status in axios headers
   */
  setAuthToken: (token) => {
    if (token) {
      localStorage.setItem('token', token)
      apiClient.defaults.headers.common['Authorization'] = `Bearer ${token}`
    } else {
      localStorage.removeItem('token')
      delete apiClient.defaults.headers.common['Authorization']
    }
  }
}

/**
 * Video API service
 */
export const videoAPI = {
  // Video CRUD
  createMetadata: (metadata) => apiClient.post('/upload/video/metadata', metadata),
  uploadFile: (videoId, formData, onProgress) => apiClient.post(`/upload/video/${videoId}/file`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    onUploadProgress: onProgress
  }),
  getVideo: (videoId) => apiClient.get(`/video/${videoId}`),
  updateVideo: (videoId, updates) => apiClient.put(`/video/${videoId}`, updates),
  getUserVideos: () => apiClient.get('/video/my-videos'),
  getVideoStatus: (videoId) => apiClient.get(`/video/${videoId}/status`)
}

/**
 * Search API service
 */
export const searchAPI = {
  searchVideos: (params) => apiClient.post('/search/videos', params),
  searchPlaylists: (params) => apiClient.post('/search/playlists', params),
  getGenres: () => apiClient.get('/search/genres'),
  getRecentVideos: (limit = 10) => apiClient.get(`/search/videos/recent?limit=${limit}`),
  getPopularPlaylists: (limit = 10) => apiClient.get(`/search/playlists/popular?limit=${limit}`),
  quickSearch: (query, page = 0, size = 20) => apiClient.get(`/search/videos/quick?q=${encodeURIComponent(query)}&page=${page}&size=${size}`)
}

/**
 * Playlist API service
 */
export const playlistAPI = {
  createPlaylist: (playlistData) => apiClient.post('/playlist', playlistData),
  getUserPlaylists: () => apiClient.get('/playlist/my-playlists'),
  addVideoToPlaylist: (playlistId, videoId, details) => apiClient.post(`/playlist/${playlistId}/videos/${videoId}`, details)
}

// Initialize with token from localStorage if available
const token = localStorage.getItem('token')
if (token) {
  apiClient.defaults.headers.common['Authorization'] = `Bearer ${token}`
}

export default {
  auth: authAPI,
  video: videoAPI,
  search: searchAPI,
  playlist: playlistAPI
}