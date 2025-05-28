import { defineStore } from 'pinia'
import axios from 'axios'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8081'

export const useVideoStore = defineStore('video', {
  state: () => ({
    videos: [],
    myVideos: [], // Explicitly track user's videos
    playlists: [],
    currentVideo: null,
    currentPlaylist: null,
    isLoading: false,
    error: null,
    uploadProgress: 0,
    videoStatusPolling: null
  }),
  
  actions: {
    // Video CRUD Operations
    async createVideo(metadata) {
      this.isLoading = true
      this.error = null
      
      try {
        const response = await axios.post(`${API_BASE_URL}/upload/video/metadata`, metadata)
        this.videos.unshift(response.data)
        return { success: true, video: response.data }
      } catch (error) {
        this.error = error.response?.data || 'Failed to create video'
        return { success: false, error: this.error }
      } finally {
        this.isLoading = false
      }
    },
    
    async uploadVideoFile(videoId, file) {
      this.isLoading = true
      this.error = null
      this.uploadProgress = 0
      
      const formData = new FormData()
      formData.append('file', file)
      
      try {
        const response = await axios.post(`${API_BASE_URL}/upload/video/${videoId}/file`, formData, {
          headers: {
            'Content-Type': 'multipart/form-data'
          },
          onUploadProgress: (progressEvent) => {
            this.uploadProgress = Math.round((progressEvent.loaded * 100) / progressEvent.total)
          }
        })
        
        return { success: true, videoId: videoId }
      } catch (error) {
        this.error = error.response?.data || 'Upload failed'
        return { success: false, error: this.error }
      } finally {
        this.isLoading = false
      }
    },
    
    async updateVideo(videoId, updates) {
      this.isLoading = true
      this.error = null
      
      try {
        const response = await axios.put(`${API_BASE_URL}/video/${videoId}`, updates)
        
        // Update local state
        const index = this.videos.findIndex(v => v.id === videoId)
        if (index !== -1) {
          this.videos[index] = response.data
        }
        
        // Also update in myVideos if present
        const myVideoIndex = this.myVideos.findIndex(v => v.id === videoId)
        if (myVideoIndex !== -1) {
          this.myVideos[myVideoIndex] = response.data
        }
        
        if (this.currentVideo?.id === videoId) {
          this.currentVideo = response.data
        }
        
        return { success: true, video: response.data }
      } catch (error) {
        this.error = error.response?.data || 'Failed to update video'
        return { success: false, error: this.error }
      } finally {
        this.isLoading = false
      }
    },
    
    async getVideo(videoId) {
      this.isLoading = true
      this.error = null
      
      try {
        console.log(`Fetching video metadata for ID: ${videoId}`)
        const response = await axios.get(`${API_BASE_URL}/video/${videoId}`)
        console.log('Video metadata response:', response.status)
        this.currentVideo = response.data
        return response.data
      } catch (error) {
        console.error('Failed to fetch video metadata:', error)
        
        // Handle specific 401/403 errors
        if (error.response && (error.response.status === 401 || error.response.status === 403)) {
          this.error = 'Authentication required to access this video'
          return { success: false, error: this.error, unauthorized: true }
        }
        
        this.error = error.response?.data || 'Failed to fetch video'
        return { success: false, error: this.error }
      } finally {
        this.isLoading = false
      }
    },
    
    async getVideoStatus(videoId) {
      try {
        const response = await axios.get(`${API_BASE_URL}/video/${videoId}/status`)
        return response.data
      } catch (error) {
        console.error('Failed to get video status:', error)
        return null
      }
    },
    
    async getMyVideos() {
      this.isLoading = true
      this.error = null
      
      try {
        const response = await axios.get(`${API_BASE_URL}/video/my-videos`)
        this.myVideos = response.data
        this.videos = [...this.myVideos] // Also update general videos list
        return this.myVideos
      } catch (error) {
        if (error.response?.status === 401) {
          this.error = 'Authentication required to view your videos'
          return { success: false, error: this.error, unauthorized: true }
        } else {
          this.error = error.response?.data || 'Failed to fetch your videos'
          return { success: false, error: this.error }
        }
      } finally {
        this.isLoading = false
      }
    },
    
    async getAllVideos() {
      // Fetch all videos regardless of authentication
      this.isLoading = true
      this.error = null
      
      try {
        // Use the search endpoint to get all videos
        const response = await axios.post(`${API_BASE_URL}/search/videos`, {
          query: '',
          page: 0,
          size: 50
        })
        
        if (response.data && Array.isArray(response.data.content)) {
          this.videos = response.data.content
          return this.videos
        }
        return []
      } catch (error) {
        console.error('Failed to fetch all videos:', error)
        this.error = error.response?.data || 'Failed to fetch videos'
        return { success: false, error: this.error }
      } finally {
        this.isLoading = false
      }
    },
    
    // Playlist CRUD Operations
    async createPlaylist(playlistData) {
      this.isLoading = true
      this.error = null
      
      try {
        const response = await axios.post(`${API_BASE_URL}/playlist`, playlistData)
        this.playlists.unshift(response.data)
        return { success: true, playlist: response.data }
      } catch (error) {
        if (error.response?.status === 401) {
          this.error = 'Authentication required to create playlists'
        } else {
          this.error = error.response?.data || 'Failed to create playlist'
        }
        return { success: false, error: this.error }
      } finally {
        this.isLoading = false
      }
    },
    
    async getMyPlaylists() {
      this.isLoading = true
      this.error = null
      
      try {
        const response = await axios.get(`${API_BASE_URL}/playlist/my-playlists`)
        this.playlists = response.data
        return this.playlists
      } catch (error) {
        if (error.response?.status === 401) {
          this.error = 'Authentication required to access your playlists'
        } else {
          this.error = error.response?.data || 'Failed to fetch your playlists'
        }
        return { success: false, error: this.error }
      } finally {
        this.isLoading = false
      }
    },
    
    // Playlist Video Management 
    async addVideoToPlaylist(playlistId, videoId, details = null) {
      this.isLoading = true
      this.error = null
      
      try {
        const response = await axios.post(
          `${API_BASE_URL}/playlist/${playlistId}/videos/${videoId}`,
          details
        )
        
        // Update local video state if available
        const videoIndex = this.videos.findIndex(v => v.id === videoId)
        if (videoIndex !== -1) {
          this.videos[videoIndex].playlistId = playlistId
          if (details) {
            if (details.seasonNumber !== undefined) {
              this.videos[videoIndex].seasonNumber = details.seasonNumber
            }
            if (details.episodeNumber !== undefined) {
              this.videos[videoIndex].episodeNumber = details.episodeNumber
            }
          }
        }
        
        return { success: true }
      } catch (error) {
        if (error.response?.status === 401) {
          this.error = 'Authentication required to modify playlists'
        } else if (error.response?.status === 403) {
          this.error = 'You do not have permission to modify this playlist'
        } else {
          this.error = error.response?.data || 'Failed to add video to playlist'
        }
        return { success: false, error: this.error }
      } finally {
        this.isLoading = false
      }
    },
    
    // Video conversion monitoring
    async pollVideoConversionStatus(videoId, interval = 2000, maxAttempts = 60) {
      return new Promise((resolve, reject) => {
        let attempts = 0
        let lastStatus = null
        
        const poll = async () => {
          try {
            const status = await this.getVideoStatus(videoId)
            
            if (!status) {
              attempts++
              if (attempts >= maxAttempts) {
                reject(new Error('Max polling attempts reached'))
                return
              }
              this.videoStatusPolling = setTimeout(poll, interval)
              return
            }
            
            lastStatus = status.status || status.conversionStatus
            
            if (lastStatus === 'COMPLETED') {
              resolve(status)
              return
            } else if (lastStatus === 'FAILED') {
              reject(new Error('Video conversion failed'))
              return
            }
            
            // Continue polling
            attempts++
            if (attempts >= maxAttempts) {
              reject(new Error('Max polling attempts reached'))
              return
            }
            
            this.videoStatusPolling = setTimeout(poll, interval)
          } catch (error) {
            attempts++
            if (attempts >= maxAttempts) {
              reject(error)
              return
            }
            this.videoStatusPolling = setTimeout(poll, interval)
          }
        }
        
        // Clear any existing polling
        if (this.videoStatusPolling) {
          clearTimeout(this.videoStatusPolling)
        }
        
        // Start polling
        poll()
      })
    },
    
    stopPolling() {
      if (this.videoStatusPolling) {
        clearTimeout(this.videoStatusPolling)
        this.videoStatusPolling = null
      }
    },
    
    // Helper methods
    isVideoProcessing(video) {
      return video && (
        video.conversionStatus === 'PENDING' || 
        video.conversionStatus === 'IN_PROGRESS' ||
        video.status === 'PENDING' ||
        video.status === 'IN_PROGRESS'
      )
    },
    
    isVideoReady(video) {
      return video && (
        video.conversionStatus === 'COMPLETED' ||
        video.status === 'COMPLETED'
      )
    },
    
    isVideoFailed(video) {
      return video && (
        video.conversionStatus === 'FAILED' ||
        video.status === 'FAILED'
      )
    },
    
    clearError() {
      this.error = null
    },
    
    clearCurrentVideo() {
      this.currentVideo = null
    }
  }
})