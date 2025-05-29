import { defineStore } from 'pinia'
import { jwtDecode } from 'jwt-decode'
import { authAPI, updateAuthHeader } from '@/services/apiService'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    user: null,
    token: localStorage.getItem('token'),
    isLoading: false,
    error: null,
    emailConfirmationMessage: null,
    emailConfirmationError: null
  }),
  
  getters: {
    isAuthenticated: (state) => !!state.token && !state.isTokenExpired,
    currentUsername: (state) => state.user?.username || null,
    userId: (state) => {
      if (!state.token) return null
      try {
        const decoded = jwtDecode(state.token)
        return decoded.sub || decoded.id || null
      } catch (e) {
        return null
      }
    },
    userEmail: (state) => state.user?.email || null,
    isTokenExpired: (state) => {
      if (!state.token) return true
      try {
        const decoded = jwtDecode(state.token)
        return decoded.exp ? decoded.exp * 1000 < Date.now() : true
      } catch (e) {
        console.error('Error decoding token:', e)
        return true
      }
    }
  },
  
  actions: {
    async register(userData) {
      this.isLoading = true
      this.error = null
      
      try {
        const response = await authAPI.register(userData)
        return { 
          success: true, 
          message: response.data.message || 'Registration successful. Please check your email for confirmation.' 
        }
      } catch (error) {
        this.handleError(error, 'Registration failed')
        return { 
          success: false, 
          error: this.error,
          message: typeof this.error === 'string' ? this.error : 'Registration failed'
        }
      } finally {
        this.isLoading = false
      }
    },
    
    async login(credentials) {
      this.isLoading = true
      this.error = null
      
      try {
        const response = await authAPI.login(credentials)
        const { jwt } = response.data
        
        if (!jwt) {
          throw new Error('Invalid login response')
        }
        
        // Set token in store AND update API client authorization headers
        this.setToken(jwt)
        
        // Extract user info from token
        this.setUserFromToken(jwt, credentials.username)
        
        console.log('[Auth] Login successful, token set')
        return { success: true }
      } catch (error) {
        this.handleError(error, 'Login failed')
        return { 
          success: false, 
          error: this.error,
          message: 'Authentication failed. Please check your credentials.'
        }
      } finally {
        this.isLoading = false
      }
    },
    
    async confirmEmail(token) {
      this.isLoading = true
      this.emailConfirmationError = null
      this.emailConfirmationMessage = null
      
      try {
        const response = await authAPI.confirmEmail(token)
        this.emailConfirmationMessage = response.data
        return { success: true, message: response.data }
      } catch (error) {
        this.emailConfirmationError = error.response?.data || 'Email confirmation failed'
        return { 
          success: false, 
          error: this.emailConfirmationError,
          message: typeof this.emailConfirmationError === 'string' ? 
            this.emailConfirmationError : 'Email confirmation failed'
        }
      } finally {
        this.isLoading = false
      }
    },
    
    async resendConfirmationEmail(email) {
      this.isLoading = true
      this.error = null
      
      try {
        const response = await authAPI.resendConfirmation(email)
        return { success: true, message: response.data }
      } catch (error) {
        this.handleError(error, 'Failed to resend confirmation email')
        return { 
          success: false, 
          error: this.error,
          message: typeof this.error === 'string' ? this.error : 'Failed to resend confirmation email'
        }
      } finally {
        this.isLoading = false
      }
    },
    
    logout() {
      console.log('[Auth] Logging out, removing token')
      this.token = null
      this.user = null
      localStorage.removeItem('token')
      localStorage.removeItem('username')
      // Ensure API client also loses the token
      updateAuthHeader(null)
    },
    
    initializeAuth() {
      console.log('[Auth] Initializing auth with token:', this.token ? 'token present' : 'no token')
      
      if (this.token) {
        // Check if token is valid and not expired
        if (!this.isTokenExpired) {
          console.log('[Auth] Token is valid, initializing')
          
          // Ensure API client has the token
          updateAuthHeader(this.token)
          
          // Restore user info from localStorage and token
          const username = localStorage.getItem('username')
          if (username) {
            this.setUserFromToken(this.token, username)
          }
        } else {
          console.log('[Auth] Token is expired, logging out')
          // If token is expired, clear it
          this.logout()
        }
      }
    },
    
    // Helper methods
    setToken(token) {
      console.log('[Auth] Setting token')
      this.token = token
      // Update API auth headers
      updateAuthHeader(token)
    },
    
    setUserFromToken(token, username) {
      try {
        console.log('[Auth] Extracting user data from token')
        const decoded = jwtDecode(token)
        this.user = { 
          username,
          email: decoded.email || null,
          roles: decoded.roles || []
        }
        localStorage.setItem('username', username)
      } catch (e) {
        console.error('[Auth] Error decoding token:', e)
        // Fallback if decode fails
        this.user = { username }
        localStorage.setItem('username', username)
      }
    },
    
    handleError(error, defaultMessage) {
      console.error('[Auth] Error:', defaultMessage, error)
      
      if (error.response && error.response.data) {
        // Handle structured error response
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
      return this.error
    }
  }
})