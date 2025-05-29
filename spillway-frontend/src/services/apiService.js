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

// Create a separate instance for search requests for debugging purposes
// We'll later merge this back into the main apiClient
const searchClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json'
  }
})

/**
 * Update all axios instances with auth token
 * @param {string|null} token - The JWT token or null to remove
 */
export const updateAuthHeader = (token) => {
  console.log("[API Service] Setting auth token:", token ? "token present" : "no token")
  
  if (token) {
    // Set token in localStorage for persistence
    localStorage.setItem('token', token)
    
    // Set the Authorization header in both clients
    apiClient.defaults.headers.common['Authorization'] = `Bearer ${token}`
    searchClient.defaults.headers.common['Authorization'] = `Bearer ${token}`
    
    // Add to global axios defaults as well
    axios.defaults.headers.common['Authorization'] = `Bearer ${token}`
    
    console.log("[API Service] Auth headers set for all clients")
  } else {
    // Clear token and headers
    localStorage.removeItem('token')
    delete apiClient.defaults.headers.common['Authorization']
    delete searchClient.defaults.headers.common['Authorization']
    delete axios.defaults.headers.common['Authorization']
    
    console.log("[API Service] Auth headers cleared for all clients")
  }
}

/**
 * Setup interceptors for debugging and error handling
 */
const setupInterceptors = () => {
  // Request interceptor for apiClient
  apiClient.interceptors.request.use(
    config => {
      console.log(`[API Request] ${config.method.toUpperCase()} ${config.url}`, 
        config.headers.Authorization ? "With Auth" : "Without Auth")
      return config
    },
    error => {
      console.error("[API Request Error]", error)
      return Promise.reject(error)
    }
  )

  // Request interceptor for searchClient
  searchClient.interceptors.request.use(
    config => {
      console.log(`[SEARCH Request] ${config.method.toUpperCase()} ${config.url}`, 
        config.headers.Authorization ? "With Auth" : "Without Auth")
      
      // Force add token from localStorage if not present in headers
      if (!config.headers.Authorization) {
        const token = localStorage.getItem('token')
        if (token) {
          console.log("[SEARCH] Adding missing Authorization header from localStorage")
          config.headers.Authorization = `Bearer ${token}`
        }
      }
      
      return config
    },
    error => {
      console.error("[SEARCH Request Error]", error)
      return Promise.reject(error)
    }
  )

  // Response interceptors for debugging
  const commonResponseInterceptor = [
    response => {
      console.log(`[API Response] ${response.status} from ${response.config.url}`)
      return response
    },
    error => {
      if (error.response) {
        console.error(`[API Error] ${error.response.status} from ${error.config.url}`, 
          error.response.data)
          
        // Handle 401 errors
        if (error.response.status === 401) {
          console.warn(`[API Unauthorized] from ${error.config.url}`)
          // We could trigger a logout or token refresh here
        }
      } else {
        console.error('[API Error] Network or other error', error)
      }
      return Promise.reject(error)
    }
  ]
  
  apiClient.interceptors.response.use(...commonResponseInterceptor)
  searchClient.interceptors.response.use(...commonResponseInterceptor)
}

// Initialize interceptors
setupInterceptors()

// Initialize with token from localStorage if available
const token = localStorage.getItem('token')
if (token) {
  console.log("[API Service] Found token in localStorage, initializing clients")
  updateAuthHeader(token)
}

/**
 * Auth API service
 */
export const authAPI = {
  login: (credentials) => apiClient.post('/auth/login', credentials),
  register: (userData) => apiClient.post('/auth/register', userData),
  confirmEmail: (token) => apiClient.get(`/auth/confirm?token=${token}`),
  resendConfirmation: (email) => apiClient.post('/auth/resend-confirmation', { email })
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
 * Search API service - using dedicated search client to ensure auth headers
 */
export const searchAPI = {
  searchVideos: (params) => searchClient.post('/search/videos', params),
  searchPlaylists: (params) => searchClient.post('/search/playlists', params),
  getGenres: () => searchClient.get('/search/genres'),
  getRecentVideos: (limit = 10) => searchClient.get(`/search/videos/recent?limit=${limit}`),
  getPopularPlaylists: (limit = 10) => searchClient.get(`/search/playlists/popular?limit=${limit}`),
  quickSearch: (query, page = 0, size = 20) => 
    searchClient.get(`/search/videos/quick?q=${encodeURIComponent(query)}&page=${page}&size=${size}`)
}

/**
 * Playlist API service
 */
export const playlistAPI = {
  createPlaylist: (playlistData) => apiClient.post('/playlist', playlistData),
  getUserPlaylists: () => apiClient.get('/playlist/my-playlists'),
  addVideoToPlaylist: (playlistId, videoId, details) => apiClient.post(`/playlist/${playlistId}/videos/${videoId}`, details)
}

export default {
  auth: authAPI,
  video: videoAPI,
  search: searchAPI,
  playlist: playlistAPI,
  updateAuthHeader
}