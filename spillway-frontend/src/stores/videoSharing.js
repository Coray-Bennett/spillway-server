import { defineStore } from 'pinia'
import { videoSharingAPI } from '@/services/apiService'

export const useVideoSharingStore = defineStore('videoSharing', {
  state: () => ({
    myCreatedShares: [],
    sharedWithMe: [],
    sharesForVideo: {},
    isLoading: false,
    error: null
  }),

  actions: {
    handleError(error, defaultMessage) {
      console.error('[Video Sharing Store]', defaultMessage, error)
      
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

    async shareVideo(shareRequest) {
      this.isLoading = true
      this.error = null

      try {
        console.log('[Video Sharing Store] Sharing video:', shareRequest)
        const response = await videoSharingAPI.shareVideo(shareRequest)
        
        // Refresh created shares to include the new one
        await this.getMyCreatedShares()
        
        return { success: true, share: response.data }
      } catch (error) {
        return this.handleError(error, 'Failed to share video')
      } finally {
        this.isLoading = false
      }
    },

    async getMyCreatedShares() {
      this.isLoading = true
      this.error = null

      try {
        console.log('[Video Sharing Store] Fetching my created shares')
        const response = await videoSharingAPI.getMyCreatedShares()
        this.myCreatedShares = response.data
        
        return { success: true, shares: response.data }
      } catch (error) {
        return this.handleError(error, 'Failed to fetch created shares')
      } finally {
        this.isLoading = false
      }
    },

    async getSharedWithMe() {
      this.isLoading = true
      this.error = null

      try {
        console.log('[Video Sharing Store] Fetching videos shared with me')
        const response = await videoSharingAPI.getSharedWithMe()
        this.sharedWithMe = response.data
        
        return { success: true, shares: response.data }
      } catch (error) {
        return this.handleError(error, 'Failed to fetch shared videos')
      } finally {
        this.isLoading = false
      }
    },

    async getSharesForVideo(videoId) {
      this.isLoading = true
      this.error = null

      try {
        console.log('[Video Sharing Store] Fetching shares for video:', videoId)
        const response = await videoSharingAPI.getSharesForVideo(videoId)
        
        // Store shares for this video
        this.sharesForVideo[videoId] = response.data
        
        return { success: true, shares: response.data }
      } catch (error) {
        return this.handleError(error, 'Failed to fetch video shares')
      } finally {
        this.isLoading = false
      }
    },

    async revokeShare(shareId) {
      this.isLoading = true
      this.error = null

      try {
        console.log('[Video Sharing Store] Revoking share:', shareId)
        await videoSharingAPI.revokeShare(shareId)
        
        // Remove from created shares
        this.myCreatedShares = this.myCreatedShares.filter(share => share.id !== shareId)
        
        // Remove from shared with me
        this.sharedWithMe = this.sharedWithMe.filter(share => share.id !== shareId)
        
        // Remove from video-specific shares
        Object.keys(this.sharesForVideo).forEach(videoId => {
          this.sharesForVideo[videoId] = this.sharesForVideo[videoId].filter(share => share.id !== shareId)
        })
        
        return { success: true }
      } catch (error) {
        return this.handleError(error, 'Failed to revoke share')
      } finally {
        this.isLoading = false
      }
    },

    clearError() {
      this.error = null
    },

    clearShares() {
      this.myCreatedShares = []
      this.sharedWithMe = []
      this.sharesForVideo = {}
      this.error = null
    }
  },

  getters: {
    getSharesForVideoId: (state) => (videoId) => {
      return state.sharesForVideo[videoId] || []
    },

    hasSharedVideos: (state) => {
      return state.sharedWithMe.length > 0
    },

    hasCreatedShares: (state) => {
      return state.myCreatedShares.length > 0
    }
  }
})