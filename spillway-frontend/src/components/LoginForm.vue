<template>
  <div class="login-form-container">
    <div v-if="success" class="success-message">
      {{ successMessage }}
      <button @click="redirectToHome" class="primary-button">Go to Home</button>
    </div>
    
    <form v-else @submit.prevent="handleSubmit" class="login-form" :class="{ 'is-loading': isLoading }">
      <h2>Login to Spillway</h2>
      
      <div v-if="error" class="error-message">
        {{ errorMessage }}
      </div>
      
      <div class="form-group">
        <label for="username">Username</label>
        <input
          type="text"
          id="username"
          v-model="credentials.username"
          required
          :disabled="isLoading"
          autocomplete="username"
        />
      </div>
      
      <div class="form-group">
        <label for="password">Password</label>
        <input
          type="password"
          id="password"
          v-model="credentials.password"
          required
          :disabled="isLoading"
          autocomplete="current-password"
        />
      </div>
      
      <div class="form-actions">
        <button type="submit" class="login-button" :disabled="isLoading">
          <span v-if="isLoading" class="loading-spinner"></span>
          {{ isLoading ? 'Logging in...' : 'Login' }}
        </button>
      </div>
      
      <div class="additional-actions">
        <router-link to="/register" class="register-link">
          Don't have an account? Register
        </router-link>
        <router-link to="/forgot-password" class="forgot-password-link">
          Forgot password?
        </router-link>
      </div>
      
      <div class="email-confirmation" v-if="showEmailConfirmationPrompt">
        <p>Haven't received confirmation email?</p>
        <form @submit.prevent="handleResendConfirmation">
          <div class="form-group">
            <input 
              type="email" 
              v-model="confirmationEmail"
              placeholder="Enter your email"
              required
            />
          </div>
          <button type="submit" :disabled="isResending" class="resend-button">
            {{ isResending ? 'Sending...' : 'Resend Confirmation Email' }}
          </button>
          <div v-if="confirmationMessage" class="confirmation-message">
            {{ confirmationMessage }}
          </div>
        </form>
      </div>
    </form>
  </div>
</template>

<script>
import { useAuthStore } from '../stores/auth'

export default {
  name: 'LoginForm',
  
  setup() {
    const authStore = useAuthStore()
    return { authStore }
  },
  
  data() {
    return {
      credentials: {
        username: '',
        password: ''
      },
      isLoading: false,
      errorMessage: '',
      successMessage: '',
      success: false,
      confirmationEmail: '',
      isResending: false,
      confirmationMessage: '',
      showEmailConfirmationPrompt: false
    }
  },
  
  computed: {
    error() {
      return this.authStore.error || this.errorMessage
    },
    isAuthenticated() {
      return this.authStore.isAuthenticated
    }
  },
  
  methods: {
    async handleSubmit() {
      this.errorMessage = ''
      this.isLoading = true
      
      try {
        const result = await this.authStore.login({
          username: this.credentials.username,
          password: this.credentials.password
        })
        
        if (result.success) {
          this.success = true
          this.successMessage = 'Login successful! Redirecting...'
          
          // Redirect after a short delay
          setTimeout(() => {
            const redirectPath = this.$route.query.redirect || '/'
            this.$router.push(redirectPath)
          }, 2000)
        } else {
          this.errorMessage = result.message || 'Login failed'
          
          // Show email confirmation prompt for specific error messages
          if (this.errorMessage.includes('confirmed') || 
              this.errorMessage.includes('confirmation')) {
            this.showEmailConfirmationPrompt = true
          }
        }
      } catch (error) {
        console.error('Login error:', error)
        this.errorMessage = 'An unexpected error occurred. Please try again.'
      } finally {
        this.isLoading = false
      }
    },
    
    async handleResendConfirmation() {
      if (!this.confirmationEmail) return
      
      this.isResending = true
      this.confirmationMessage = ''
      
      try {
        const result = await this.authStore.resendConfirmationEmail(this.confirmationEmail)
        this.confirmationMessage = result.message || 'Confirmation email sent!'
      } catch (error) {
        console.error('Failed to resend confirmation:', error)
        this.confirmationMessage = 'Failed to send email. Please try again later.'
      } finally {
        this.isResending = false
      }
    },
    
    redirectToHome() {
      this.$router.push('/')
    }
  },
  
  watch: {
    isAuthenticated(newVal) {
      if (newVal) {
        const redirectPath = this.$route.query.redirect || '/'
        this.$router.push(redirectPath)
      }
    }
  }
}
</script>

<style scoped>
.login-form-container {
  max-width: 400px;
  margin: 2rem auto;
  padding: 2rem;
  background-color: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 10px rgba(0,0,0,0.1);
}

.login-form {
  display: flex;
  flex-direction: column;
}

.login-form.is-loading {
  opacity: 0.8;
}

h2 {
  margin-bottom: 1.5rem;
  color: #0066cc;
  text-align: center;
}

.form-group {
  margin-bottom: 1.5rem;
}

label {
  display: block;
  margin-bottom: 0.5rem;
  color: #333;
  font-weight: 500;
}

input {
  width: 100%;
  padding: 0.8rem;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 1rem;
}

.form-actions {
  margin-top: 1rem;
}

.login-button {
  width: 100%;
  padding: 0.8rem;
  background-color: #0066cc;
  color: white;
  border: none;
  border-radius: 4px;
  font-size: 1.1rem;
  cursor: pointer;
  transition: background-color 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
}

.login-button:hover:not(:disabled) {
  background-color: #0055aa;
}

.login-button:disabled {
  background-color: #cccccc;
  cursor: not-allowed;
}

.additional-actions {
  margin-top: 1.5rem;
  display: flex;
  justify-content: space-between;
}

.register-link, .forgot-password-link {
  color: #0066cc;
  text-decoration: none;
}

.register-link:hover, .forgot-password-link:hover {
  text-decoration: underline;
}

.error-message {
  background-color: #ffeeee;
  color: #cc0000;
  padding: 0.8rem;
  border-radius: 4px;
  margin-bottom: 1.5rem;
}

.success-message {
  background-color: #eeffee;
  color: #00aa00;
  padding: 0.8rem;
  border-radius: 4px;
  margin-bottom: 1.5rem;
  text-align: center;
}

.loading-spinner {
  width: 1.2rem;
  height: 1.2rem;
  border: 2px solid rgba(255,255,255, 0.3);
  border-radius: 50%;
  border-top-color: white;
  animation: spin 0.8s ease-in-out infinite;
  margin-right: 0.5rem;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.email-confirmation {
  margin-top: 2rem;
  padding-top: 1rem;
  border-top: 1px solid #eee;
}

.email-confirmation p {
  margin-bottom: 0.8rem;
  color: #666;
}

.resend-button {
  margin-top: 0.5rem;
  padding: 0.5rem 1rem;
  background-color: #666;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}

.resend-button:hover:not(:disabled) {
  background-color: #444;
}

.confirmation-message {
  margin-top: 0.8rem;
  color: #00aa00;
}

.primary-button {
  margin-top: 1rem;
  padding: 0.6rem 1rem;
  background-color: #0066cc;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}

.primary-button:hover {
  background-color: #0055aa;
}
</style>