<template>
    <form @submit.prevent="handleRegister" class="register-form">
      <div class="form-group">
        <label for="username" class="form-label">Username</label>
        <input
          id="username"
          v-model="form.username"
          type="text"
          class="form-input"
          placeholder="Choose a username"
          required
        />
      </div>
      
      <div class="form-group">
        <label for="email" class="form-label">Email</label>
        <input
          id="email"
          v-model="form.email"
          type="email"
          class="form-input"
          placeholder="Enter your email"
          required
        />
      </div>
      
      <div class="form-group">
        <label for="password" class="form-label">Password</label>
        <input
          id="password"
          v-model="form.password"
          type="password"
          class="form-input"
          placeholder="Create a password"
          required
        />
      </div>
      
      <div v-if="error" class="error-text">{{ error }}</div>
      <div v-if="successMessage" class="success-text">{{ successMessage }}</div>
      
      <button type="submit" class="btn btn-primary w-full" :disabled="isLoading">
        {{ isLoading ? 'Creating account...' : 'Create Account' }}
      </button>
    </form>
  </template>
  
  <script setup>
  import { ref } from 'vue'
  import { useRouter } from 'vue-router'
  import { useAuthStore } from '../stores/auth'
  
  const authStore = useAuthStore()
  const router = useRouter()
  
  const form = ref({
    username: '',
    email: '',
    password: ''
  })
  
  const error = ref('')
  const successMessage = ref('')
  const isLoading = ref(false)
  
  async function handleRegister() {
    isLoading.value = true
    error.value = ''
    successMessage.value = ''
    
    const result = await authStore.register(form.value)
    
    if (result.success) {
      successMessage.value = 'Account created successfully! Redirecting to login...'
      setTimeout(() => {
        router.push('/auth/login')
      }, 1500)
    } else {
      error.value = result.error
    }
    
    isLoading.value = false
  }
  </script>
  
  <style scoped>
  .form-group {
    margin-bottom: 1.25rem;
  }
  
  .w-full {
    width: 100%;
  }
  
  .register-form {
    margin-top: 1rem;
  }
  </style>