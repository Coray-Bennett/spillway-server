<template>
  <div class="email-confirmation-view">
    <div class="container">
      <div class="confirmation-card">
        <div class="card-header">
          <BaseIcon name="mail" :size="48" class="icon" />
          <h1>Email Confirmation</h1>
        </div>

        <!-- Loading State -->
        <div v-if="authStore.isLoading" class="loading-state">
          <div class="spinner"></div>
          <p>Confirming your email...</p>
        </div>

        <!-- Success State -->
        <div v-else-if="confirmationSuccess" class="success-state">
          <BaseIcon name="check-circle" :size="48" class="success-icon" />
          <h2>Email Confirmed Successfully!</h2>
          <p>{{ authStore.emailConfirmationMessage }}</p>
          <router-link to="/auth/login" class="btn btn-primary">
            Continue to Login
          </router-link>
        </div>

        <!-- Error State -->
        <div v-else-if="authStore.emailConfirmationError" class="error-state">
          <BaseIcon name="x-circle" :size="48" class="error-icon" />
          <h2>Confirmation Failed</h2>
          <p>{{ authStore.emailConfirmationError }}</p>
          
          <div class="resend-section">
            <p>Need a new confirmation email?</p>
            <form @submit.prevent="resendConfirmation" class="resend-form">
              <div class="form-group">
                <input
                  v-model="email"
                  type="email"
                  placeholder="Enter your email address"
                  class="form-input"
                  required
                />
              </div>
              <button 
                type="submit" 
                :disabled="authStore.isLoading || !email.trim()"
                class="btn btn-outline"
              >
                <span v-if="authStore.isLoading">Sending...</span>
                <span v-else>Resend Confirmation Email</span>
              </button>
            </form>
          </div>
        </div>

        <!-- Initial State (no token provided) -->
        <div v-else class="initial-state">
          <h2>Confirm Your Email</h2>
          <p>Please check your email and click the confirmation link to activate your account.</p>
          
          <div class="resend-section">
            <p>Didn't receive the email?</p>
            <form @submit.prevent="resendConfirmation" class="resend-form">
              <div class="form-group">
                <label for="email">Email Address</label>
                <input
                  id="email"
                  v-model="email"
                  type="email"
                  placeholder="Enter your email address"
                  class="form-input"
                  required
                />
              </div>
              <button 
                type="submit" 
                :disabled="authStore.isLoading || !email.trim()"
                class="btn btn-primary"
              >
                <span v-if="authStore.isLoading">Sending...</span>
                <span v-else>Resend Confirmation Email</span>
              </button>
            </form>
          </div>
        </div>

        <!-- Success message for resend -->
        <div v-if="resendSuccess" class="success-message">
          {{ resendSuccess }}
        </div>

        <!-- Error message for resend -->
        <div v-if="resendError" class="error-message">
          {{ resendError }}
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import BaseIcon from '@/components/icons/BaseIcon.vue'

const route = useRoute()
const authStore = useAuthStore()

const confirmationSuccess = ref(false)
const email = ref('')
const resendSuccess = ref('')
const resendError = ref('')

onMounted(async () => {
  const token = route.query.token
  
  if (token) {
    const result = await authStore.confirmEmail(token)
    if (result.success) {
      confirmationSuccess.value = true
    }
  }
})

async function resendConfirmation() {
  resendSuccess.value = ''
  resendError.value = ''
  
  const result = await authStore.resendConfirmationEmail(email.value)
  
  if (result.success) {
    resendSuccess.value = result.message || 'Confirmation email sent successfully!'
    email.value = ''
  } else {
    resendError.value = result.message || 'Failed to send confirmation email'
  }
}
</script>

<style scoped>
.email-confirmation-view {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 2rem 1rem;
  background: linear-gradient(135deg, var(--primary-bg) 0%, var(--secondary-bg) 100%);
}

.container {
  width: 100%;
  max-width: 500px;
}

.confirmation-card {
  background-color: var(--secondary-bg);
  border: 1px solid var(--border-color);
  border-radius: 1rem;
  padding: 2rem;
  text-align: center;
  box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.1);
}

.card-header {
  margin-bottom: 2rem;
}

.card-header h1 {
  margin: 1rem 0 0 0;
  color: var(--primary-text);
  font-size: 1.875rem;
  font-weight: 700;
}

.icon {
  color: var(--accent-color);
}

.loading-state, .success-state, .error-state, .initial-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 1rem;
}

.spinner {
  width: 32px;
  height: 32px;
  border: 3px solid var(--border-color);
  border-top: 3px solid var(--accent-color);
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.success-icon {
  color: var(--success-color);
}

.error-icon {
  color: var(--danger-color);
}

.success-state h2, .error-state h2, .initial-state h2 {
  margin: 0;
  color: var(--primary-text);
  font-size: 1.5rem;
}

.success-state p, .error-state p, .initial-state p {
  margin: 0;
  color: var(--secondary-text);
  line-height: 1.6;
}

.resend-section {
  margin-top: 2rem;
  padding-top: 1.5rem;
  border-top: 1px solid var(--border-color);
  width: 100%;
}

.resend-form {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  margin-top: 1rem;
}

.form-group {
  text-align: left;
}

.form-group label {
  display: block;
  margin-bottom: 0.5rem;
  font-weight: 500;
  color: var(--primary-text);
}

.form-input {
  width: 100%;
  padding: 0.75rem;
  border: 1px solid var(--border-color);
  border-radius: 0.5rem;
  background-color: var(--primary-bg);
  color: var(--primary-text);
  transition: var(--transition);
}

.form-input:focus {
  outline: none;
  border-color: var(--accent-color);
  box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.1);
}

.btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  padding: 0.75rem 1.5rem;
  border: none;
  border-radius: 0.5rem;
  font-weight: 600;
  text-decoration: none;
  cursor: pointer;
  transition: var(--transition);
  font-size: 0.875rem;
}

.btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-primary {
  background-color: var(--accent-color);
  color: white;
}

.btn-primary:hover:not(:disabled) {
  background-color: var(--accent-hover);
}

.btn-outline {
  background-color: transparent;
  color: var(--accent-color);
  border: 1px solid var(--accent-color);
}

.btn-outline:hover:not(:disabled) {
  background-color: var(--accent-color);
  color: white;
}

.success-message {
  background-color: rgba(34, 197, 94, 0.1);
  border: 1px solid rgba(34, 197, 94, 0.3);
  color: var(--success-color);
  padding: 0.75rem;
  border-radius: 0.5rem;
  margin-top: 1rem;
}

.error-message {
  background-color: rgba(239, 68, 68, 0.1);
  border: 1px solid rgba(239, 68, 68, 0.3);
  color: var(--danger-color);
  padding: 0.75rem;
  border-radius: 0.5rem;
  margin-top: 1rem;
}

@media (max-width: 768px) {
  .email-confirmation-view {
    padding: 1rem 0.5rem;
  }
  
  .confirmation-card {
    padding: 1.5rem;
  }
  
  .card-header h1 {
    font-size: 1.5rem;
  }
}
</style>