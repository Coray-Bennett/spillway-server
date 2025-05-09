<template>
    <form @submit.prevent="handleLogin" class="login-form">
      <div class="form-group">
        <label for="username" class="form-label">Username</label>
        <input
          id="username"
          v-model="form.username"
          type="text"
          class="form-input"
          placeholder="Enter your username"
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
          placeholder="Enter your password"
          required
        />
      </div>
      
      <div v-if="error" class="error-text">{{ error }}</div>
      
      <button type="submit" class="btn btn-primary w-full" :disabled="isLoading">
        {{ isLoading ? 'Signing in...' : 'Sign In' }}
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
    password: ''
  })
  
  const error = ref('')
  const isLoading = ref(false)
  
  async function handleLogin() {
    isLoading.value = true
    error.value = ''
    
    const result = await authStore.login(form.value)
    
    if (result.success) {
      router.push('/')
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
  
  .login-form {
    margin-top: 1rem;
  }
  </style>