import { defineStore } from 'pinia'
import axios from 'axios'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8081'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    user: null,
    token: localStorage.getItem('token'),
    isLoading: false,
    error: null
  }),
  
  getters: {
    isAuthenticated: (state) => !!state.token,
    currentUsername: (state) => state.user?.username || null,
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
        
        return { success: true, message: response.data }
      } catch (error) {
        this.error = error.response?.data || 'Registration failed'
        return { success: false, error: this.error }
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
        
        this.token = response.data.jwt
        localStorage.setItem('token', this.token)
        axios.defaults.headers.common['Authorization'] = `Bearer ${this.token}`
        
        // Decode JWT to get user info or just store username
        this.user = { username }
        localStorage.setItem('username', username)
        
        return { success: true }
      } catch (error) {
        this.error = error.response?.data || 'Login failed'
        return { success: false, error: this.error }
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
        axios.defaults.headers.common['Authorization'] = `Bearer ${this.token}`
        // Restore username from localStorage
        const username = localStorage.getItem('username')
        if (username) {
          this.user = { username }
        }
      }
    }
  }
})