import { defineStore } from 'pinia'
import { playlistAPI } from '@/services/apiService'

export const usePlaylistStore = defineStore('playlist', {
  state: () => ({
    playlists: [],
    currentPlaylist: null,
    playlistVideos: {},
    isLoading: false,
    error: null
  }),

  actions: {
    handleError(error, defaultMessage) {
      console.error('[Playlist Store]', defaultMessage, error)
      
      if (error.response?.data) {
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

    async createPlaylist(playlistData) {
      this.isLoading = true
      this.error = null

      try {
        console.log('[Playlist Store] Creating playlist:', playlistData)
        const response = await playlistAPI.createPlaylist(playlistData)
        
        // Add to our playlists
        this.playlists.unshift(response.data)
        
        return { success: true, playlist: response.data }
      } catch (error) {
        return this.handleError(error, 'Failed to create playlist')
      } finally {
        this.isLoading = false
      }
    },

    async getUserPlaylists() {
      this.isLoading = true
      this.error = null

      try {
        console.log('[Playlist Store] Fetching user playlists')
        const response = await playlistAPI.getUserPlaylists()
        this.playlists = response.data
        
        return { success: true, playlists: response.data }
      } catch (error) {
        return this.handleError(error, 'Failed to fetch playlists')
      } finally {
        this.isLoading = false
      }
    },

    async getPlaylist(playlistId) {
      this.isLoading = true
      this.error = null

      try {
        console.log('[Playlist Store] Fetching playlist:', playlistId)
        const response = await playlistAPI.getPlaylist(playlistId)
        this.currentPlaylist = response.data
        
        return { success: true, playlist: response.data }
      } catch (error) {
        return this.handleError(error, 'Failed to fetch playlist')
      } finally {
        this.isLoading = false
      }
    },

    async getPlaylistVideos(playlistId) {
      this.isLoading = true
      this.error = null

      try {
        console.log('[Playlist Store] Fetching playlist videos:', playlistId)
        const response = await playlistAPI.getPlaylistVideos(playlistId)
        
        // Store videos for this playlist
        this.playlistVideos[playlistId] = response.data
        
        return { success: true, videos: response.data }
      } catch (error) {
        return this.handleError(error, 'Failed to fetch playlist videos')
      } finally {
        this.isLoading = false
      }
    },

    async updatePlaylist(playlistId, playlistData) {
      this.isLoading = true
      this.error = null

      try {
        console.log('[Playlist Store] Updating playlist:', playlistId, playlistData)
        const response = await playlistAPI.updatePlaylist(playlistId, playlistData)
        
        // Update in our playlists array
        const index = this.playlists.findIndex(p => p.id === playlistId)
        if (index !== -1) {
          this.playlists[index] = response.data
        }
        
        // Update current playlist if it's the same
        if (this.currentPlaylist?.id === playlistId) {
          this.currentPlaylist = response.data
        }
        
        return { success: true, playlist: response.data }
      } catch (error) {
        return this.handleError(error, 'Failed to update playlist')
      } finally {
        this.isLoading = false
      }
    },

    async addVideoToPlaylist(playlistId, videoId, details = null) {
      this.isLoading = true
      this.error = null

      try {
        console.log('[Playlist Store] Adding video to playlist:', { playlistId, videoId, details })
        await playlistAPI.addVideoToPlaylist(playlistId, videoId, details)
        
        // Refresh playlist videos
        await this.getPlaylistVideos(playlistId)
        
        return { success: true }
      } catch (error) {
        return this.handleError(error, 'Failed to add video to playlist')
      } finally {
        this.isLoading = false
      }
    },

    async removeVideoFromPlaylist(playlistId, videoId) {
      this.isLoading = true
      this.error = null

      try {
        console.log('[Playlist Store] Removing video from playlist:', { playlistId, videoId })
        await playlistAPI.removeVideoFromPlaylist(playlistId, videoId)
        
        // Remove from our local cache
        if (this.playlistVideos[playlistId]) {
          this.playlistVideos[playlistId] = this.playlistVideos[playlistId].filter(v => v.id !== videoId)
        }
        
        return { success: true }
      } catch (error) {
        return this.handleError(error, 'Failed to remove video from playlist')
      } finally {
        this.isLoading = false
      }
    },

    clearError() {
      this.error = null
    },

    clearCurrentPlaylist() {
      this.currentPlaylist = null
    },

    clearAll() {
      this.playlists = []
      this.currentPlaylist = null
      this.playlistVideos = {}
      this.error = null
    }
  },

  getters: {
    getPlaylistById: (state) => (playlistId) => {
      return state.playlists.find(p => p.id === playlistId)
    },

    getVideosForPlaylist: (state) => (playlistId) => {
      return state.playlistVideos[playlistId] || []
    },

    playlistCount: (state) => {
      return state.playlists.length
    }
  }
})