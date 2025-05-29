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
 * Update all axios instances with auth token
 * @param {string|null} token - The JWT token or null to remove
 */
export const updateAuthHeader = (token) => {
  if (token) {
    localStorage.setItem('token', token)

    apiClient.defaults.headers.common['Authorization'] = `Bearer ${token}`
    axios.defaults.headers.common['Authorization'] = `Bearer ${token}`
  } else {
    // Clear token and headers
    localStorage.removeItem('token')
    delete apiClient.defaults.headers.common['Authorization']
    delete axios.defaults.headers.common['Authorization']
  }
}

/**
 * Setup interceptors for debugging and error handling
 */
const setupInterceptors = () => {
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
}

setupInterceptors()

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
  searchVideos: (params) => apiClient.post('/search/videos', params),
  searchPlaylists: (params) => apiClient.post('/search/playlists', params),
  getGenres: () => apiClient.get('/search/genres'),
  getRecentVideos: (limit = 10) => apiClient.get(`/search/videos/recent?limit=${limit}`),
  getPopularPlaylists: (limit = 10) => apiClient.get(`/search/playlists/popular?limit=${limit}`),
  quickSearch: (query, page = 0, size = 20) => 
    apiClient.get(`/search/videos/quick?q=${encodeURIComponent(query)}&page=${page}&size=${size}`)
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