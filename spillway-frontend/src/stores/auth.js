import { defineStore } from 'pinia'
import axios from 'axios'
import { jwtDecode } from 'jwt-decode'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8081'

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
    isAuthenticated: (state) => !!state.token,
    currentUsername: (state) => state.user?.username || null,
    userId: (state) => {
      if (!state.token) return null;
      try {
        const decoded = jwtDecode(state.token);
        return decoded.sub || decoded.id || null;
      } catch (e) {
        return null;
      }
    },
    userEmail: (state) => state.user?.email || null,
    isTokenExpired: (state) => {
      if (!state.token) return true;
      try {
        const decoded = jwtDecode(state.token);
        // Check if token is expired (exp is in seconds)
        return decoded.exp ? decoded.exp * 1000 < Date.now() : true;
      } catch (e) {
        return true;
      }
    }
  },
  
  actions: {
    async register({ username, email, password }) {
      this.isLoading = true
      this.error = null
      
      try {
        const response = await axios.post(`${API_BASE_URL}/auth/register`, {
          username,
          email,
          password
        })
        
        return { 
          success: true, 
          message: response.data.message || 'Registration successful. Please check your email for confirmation.' 
        }
      } catch (error) {
        this.error = error.response?.data || 'Registration failed'
        return { 
          success: false, 
          error: this.error,
          message: typeof this.error === 'string' ? this.error : 'Registration failed'
        }
      } finally {
        this.isLoading = false
      }
    },
    
    async login({ username, password }) {
      this.isLoading = true
      this.error = null
      
      try {
        const response = await axios.post(`${API_BASE_URL}/auth/login`, {
          username,
          password
        })
        
        const { jwt } = response.data
        
        if (!jwt) {
          throw new Error('Invalid login response')
        }
        
        this.token = jwt
        localStorage.setItem('token', jwt)
        this.updateAxiosAuthorization()
        
        // Decode JWT to get user info
        try {
          const decoded = jwtDecode(jwt)
          this.user = { 
            username,
            email: decoded.email || null,
            roles: decoded.roles || []
          }
          localStorage.setItem('username', username)
        } catch (e) {
          // Fallback if decode fails
          this.user = { username }
          localStorage.setItem('username', username)
        }
        
        return { success: true }
      } catch (error) {
        this.error = error.response?.data || 'Login failed'
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
        const response = await axios.get(`${API_BASE_URL}/auth/confirm?token=${token}`)
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
        const response = await axios.post(`${API_BASE_URL}/auth/resend-confirmation`, { email })
        return { success: true, message: response.data }
      } catch (error) {
        this.error = error.response?.data || 'Failed to resend confirmation email'
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
      this.token = null
      this.user = null
      localStorage.removeItem('token')
      localStorage.removeItem('username')
      delete axios.defaults.headers.common['Authorization']
    },
    
    initializeAuth() {
      if (this.token) {
        // Check if token is expired
        if (!this.isTokenExpired) {
          this.updateAxiosAuthorization()
          // Restore username from localStorage
          const username = localStorage.getItem('username')
          if (username) {
            try {
              const decoded = jwtDecode(this.token)
              this.user = { 
                username,
                email: decoded.email || null,
                roles: decoded.roles || []
              }
            } catch (e) {
              this.user = { username }
            }
          }
        } else {
          // If token is expired, clear it
          this.logout()
        }
      }
    },
    
    updateAxiosAuthorization() {
      if (this.token) {
        axios.defaults.headers.common['Authorization'] = `Bearer ${this.token}`
      } else {
        delete axios.defaults.headers.common['Authorization']
      }
    },
    
    // Add interceptor for token refresh or errors
    setupAxiosInterceptors() {
      // Request interceptor
      axios.interceptors.request.use(
        config => {
          // Add token if available
          if (this.token && !this.isTokenExpired) {
            config.headers['Authorization'] = `Bearer ${this.token}`
          }
          return config
        },
        error => {
          return Promise.reject(error)
        }
      )
      
      // Response interceptor
      axios.interceptors.response.use(
        response => response,
        error => {
          if (error.response && error.response.status === 401) {
            // Unauthorized - token may be expired
            this.logout()
            // Redirect to login could be done here if we had router
          }
          return Promise.reject(error)
        }
      )
    }
  }
})