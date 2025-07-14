import { defineStore } from 'pinia'
import { videoAPI, playlistAPI } from '@/services/apiService'

export const useVideoStore = defineStore('video', {
  state: () => ({
    videos: [],
    myVideos: [],
    playlists: [],
    currentVideo: null,
    currentPlaylist: null,
    isLoading: false,
    error: null,
    uploadProgress: 0,
    videoStatusPolling: null
  }),
  
  getters: {
    // Helper methods as getters to simplify component template usage
    isVideoProcessing: () => (video) => {
      return video && (
        video.conversionStatus === 'PENDING' || 
        video.conversionStatus === 'IN_PROGRESS' ||
        video.status === 'PENDING' ||
        video.status === 'IN_PROGRESS'
      )
    },
    
    isVideoReady: () => (video) => {
      return video && (
        video.conversionStatus === 'COMPLETED' ||
        video.status === 'COMPLETED'
      )
    },
    
    isVideoFailed: () => (video) => {
      return video && (
        video.conversionStatus === 'FAILED' ||
        video.status === 'FAILED'
      )
    }
  },
  
  actions: {
    // Error handling helper
    handleError(error, defaultMessage) {
      this.error = error.response?.data || defaultMessage
      console.error(defaultMessage, error)
      return { success: false, error: this.error }
    },
    
    // Video CRUD Operations
    async createVideo(metadata) {
      this.isLoading = true
      this.error = null
      
      try {
        const response = await videoAPI.createMetadata(metadata)
        this.videos.unshift(response.data)
        return { success: true, video: response.data }
      } catch (error) {
        return this.handleError(error, 'Failed to create video')
      } finally {
        this.isLoading = false
      }
    },
    
    async uploadVideoFile(videoId, file, encryptionKey) {
      this.isLoading = true
      this.error = null
      this.uploadProgress = 0
      
      const formData = new FormData()
      formData.append('file', file)
      
      try {
        const onProgress = (progressEvent) => {
          this.uploadProgress = Math.round((progressEvent.loaded * 100) / progressEvent.total)
        }
        
        await videoAPI.uploadFile(videoId, formData, onProgress, encryptionKey)
        
        return { success: true, videoId }
      } catch (error) {
        return this.handleError(error, 'Upload failed')
      } finally {
        this.isLoading = false
      }
    },
    
    async updateVideo(videoId, updates) {
      this.isLoading = true
      this.error = null
      
      try {
        const response = await videoAPI.updateVideo(videoId, updates)
        
        // Update local state in all places
        this.updateVideoInArrays(videoId, response.data)
        
        return { success: true, video: response.data }
      } catch (error) {
        return this.handleError(error, 'Failed to update video')
      } finally {
        this.isLoading = false
      }
    },

    // Helper to update video in all arrays
    updateVideoInArrays(videoId, updatedVideo) {
      const arrays = [
        { array: this.videos, setter: (arr, idx, video) => arr[idx] = video },
        { array: this.myVideos, setter: (arr, idx, video) => arr[idx] = video }
      ]
      
      for (const { array, setter } of arrays) {
        const index = array.findIndex(v => v.id === videoId)
        if (index !== -1) {
          setter(array, index, updatedVideo)
        }
      }
      
      if (this.currentVideo?.id === videoId) {
        this.currentVideo = updatedVideo
      }
    },
    
    async getVideo(videoId) {
      this.isLoading = true
      this.error = null
      
      try {
        const response = await videoAPI.getVideo(videoId)
        this.currentVideo = response.data
        return response.data
      } catch (error) {
        // Handle specific 401/403 errors
        if (error.response && (error.response.status === 401 || error.response.status === 403)) {
          this.error = 'Authentication required to access this video'
          return { success: false, error: this.error, unauthorized: true }
        }
        
        return this.handleError(error, 'Failed to fetch video')
      } finally {
        this.isLoading = false
      }
    },
    
    async getVideoStatus(videoId) {
      try {
        const response = await videoAPI.getVideoStatus(videoId)
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
        const response = await videoAPI.getUserVideos()
        this.myVideos = response.data
        return this.myVideos
      } catch (error) {
        if (error.response?.status === 401) {
          this.error = 'Authentication required to view your videos'
          return { success: false, error: this.error, unauthorized: true }
        } else {
          return this.handleError(error, 'Failed to fetch your videos')
        }
      } finally {
        this.isLoading = false
      }
    },
    
    // Playlist operations
    async createPlaylist(playlistData) {
      this.isLoading = true
      this.error = null
      
      try {
        const response = await playlistAPI.createPlaylist(playlistData)
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
        const response = await playlistAPI.getUserPlaylists()
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
        await playlistAPI.addVideoToPlaylist(playlistId, videoId, details)
        
        // Update local video state if available
        const videoIndex = this.videos.findIndex(v => v.id === videoId)
        if (videoIndex !== -1) {
          const updatedVideo = { ...this.videos[videoIndex], playlistId }
          
          if (details) {
            if (details.seasonNumber !== undefined) {
              updatedVideo.seasonNumber = details.seasonNumber
            }
            if (details.episodeNumber !== undefined) {
              updatedVideo.episodeNumber = details.episodeNumber
            }
          }
          
          this.updateVideoInArrays(videoId, updatedVideo)
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
            
            const currentStatus = status.status || status.conversionStatus
            
            if (currentStatus === 'COMPLETED') {
              resolve(status)
              return
            } else if (currentStatus === 'FAILED') {
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
        this.stopPolling()
        
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
    
    clearError() {
      this.error = null
    },
    
    clearCurrentVideo() {
      this.currentVideo = null
    }
  }
})