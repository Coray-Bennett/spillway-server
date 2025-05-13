import { defineStore } from 'pinia'
import axios from 'axios'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8081'

export const useVideoStore = defineStore('video', {
  state: () => ({
    videos: [],
    playlists: [],
    currentVideo: null,
    isLoading: false,
    error: null,
    uploadProgress: 0
  }),
  
  actions: {
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
    
    async getPlaylist(playlistId) {
      try {
        const response = await axios.get(`${API_BASE_URL}/playlist/${playlistId}`)
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

    async getMyVideos() {
      this.isLoading = true;
      this.error = null;
      
      try {
        const response = await axios.get(`${API_BASE_URL}/video/my-videos`);
        this.videos = response.data;
        return this.videos;
      } catch (error) {
        this.error = error.response?.data || 'Failed to fetch your videos';
        throw error;
      } finally {
        this.isLoading = false;
      }
    },
    
    async getMyPlaylists() {
      this.isLoading = true;
      this.error = null;
      
      try {
        const response = await axios.get(`${API_BASE_URL}/playlist/my-playlists`);
        this.playlists = response.data;
        return this.playlists;
      } catch (error) {
        this.error = error.response?.data || 'Failed to fetch your playlists';
        throw error;
      } finally {
        this.isLoading = false;
      }
    }
  }
})