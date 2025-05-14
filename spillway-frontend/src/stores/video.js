import { defineStore } from 'pinia'
import axios from 'axios'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8081'

export const useVideoStore = defineStore('video', {
  state: () => ({
    videos: [],
    playlists: [],
    currentVideo: null,
    currentPlaylist: null,
    isLoading: false,
    error: null,
    uploadProgress: 0
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
        
        return { success: true }
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
    
    async deleteVideo(videoId) {
      this.isLoading = true
      this.error = null
      
      try {
        await axios.delete(`${API_BASE_URL}/video/${videoId}`)
        
        // Remove from local state
        this.videos = this.videos.filter(v => v.id !== videoId)
        
        if (this.currentVideo?.id === videoId) {
          this.currentVideo = null
        }
        
        return { success: true }
      } catch (error) {
        this.error = error.response?.data || 'Failed to delete video'
        return { success: false, error: this.error }
      } finally {
        this.isLoading = false
      }
    },
    
    async getVideo(videoId) {
      try {
        const response = await axios.get(`${API_BASE_URL}/video/${videoId}`)
        this.currentVideo = response.data
        return response.data
      } catch (error) {
        this.error = error.response?.data || 'Failed to fetch video'
        throw error
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
        this.videos = response.data
        return this.videos
      } catch (error) {
        this.error = error.response?.data || 'Failed to fetch your videos'
        throw error
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
        this.error = error.response?.data || 'Failed to create playlist'
        return { success: false, error: this.error }
      } finally {
        this.isLoading = false
      }
    },
    
    async updatePlaylist(playlistId, updates) {
      this.isLoading = true
      this.error = null
      
      try {
        const response = await axios.put(`${API_BASE_URL}/playlist/${playlistId}`, updates)
        
        // Update local state
        const index = this.playlists.findIndex(p => p.id === playlistId)
        if (index !== -1) {
          this.playlists[index] = response.data
        }
        
        if (this.currentPlaylist?.id === playlistId) {
          this.currentPlaylist = response.data
        }
        
        return { success: true, playlist: response.data }
      } catch (error) {
        this.error = error.response?.data || 'Failed to update playlist'
        return { success: false, error: this.error }
      } finally {
        this.isLoading = false
      }
    },
    
    async deletePlaylist(playlistId) {
      this.isLoading = true
      this.error = null
      
      try {
        await axios.delete(`${API_BASE_URL}/playlist/${playlistId}`)
        
        // Remove from local state
        this.playlists = this.playlists.filter(p => p.id !== playlistId)
        
        if (this.currentPlaylist?.id === playlistId) {
          this.currentPlaylist = null
        }
        
        return { success: true }
      } catch (error) {
        this.error = error.response?.data || 'Failed to delete playlist'
        return { success: false, error: this.error }
      } finally {
        this.isLoading = false
      }
    },
    
    async getPlaylist(playlistId) {
      try {
        const response = await axios.get(`${API_BASE_URL}/playlist/${playlistId}`)
        this.currentPlaylist = response.data
        return response.data
      } catch (error) {
        this.error = error.response?.data || 'Failed to fetch playlist'
        throw error
      }
    },
    
    async getPlaylistVideos(playlistId) {
      try {
        const response = await axios.get(`${API_BASE_URL}/playlist/${playlistId}/videos`)
        return response.data
      } catch (error) {
        this.error = error.response?.data || 'Failed to fetch playlist videos'
        throw error
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
        this.error = error.response?.data || 'Failed to fetch your playlists'
        throw error
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
        this.error = error.response?.data || 'Failed to add video to playlist'
        return { success: false, error: this.error }
      } finally {
        this.isLoading = false
      }
    },
    
    async removeVideoFromPlaylist(playlistId, videoId) {
      this.isLoading = true
      this.error = null
      
      try {
        await axios.delete(`${API_BASE_URL}/playlist/${playlistId}/videos/${videoId}`)
        
        // Update local video state if available
        const videoIndex = this.videos.findIndex(v => v.id === videoId)
        if (videoIndex !== -1) {
          this.videos[videoIndex].playlistId = null
          this.videos[videoIndex].seasonNumber = null
          this.videos[videoIndex].episodeNumber = null
        }
        
        return { success: true }
      } catch (error) {
        this.error = error.response?.data || 'Failed to remove video from playlist'
        return { success: false, error: this.error }
      } finally {
        this.isLoading = false
      }
    },
    
    // Helper method to check if a video is in process
    isVideoProcessing(video) {
      return video.conversionStatus === 'PENDING' || 
             video.conversionStatus === 'IN_PROGRESS'
    },
    
    // Helper method to check if a video is ready
    isVideoReady(video) {
      return video.conversionStatus === 'COMPLETED'
    }
  }
})