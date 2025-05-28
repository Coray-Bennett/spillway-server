<template>
    <div class="auth-view">
      <!-- <div class="container">
        <div class="auth-container">
          <div class="auth-card card"> -->
            <!-- <div class="auth-header">
              <h2 class="auth-title">{{ isLogin ? 'Sign In' : 'Create Account' }}</h2>
              <p class="auth-subtitle">
                {{ isLogin ? 'Welcome back to Spillway' : 'Join Spillway today' }}
              </p>
            </div> -->
            
            <router-view />
            
            <!-- <div class="auth-footer">
              <p class="switch-text">
                {{ isLogin ? "Don't have an account?" : "Already have an account?" }}
                <router-link 
                  :to="isLogin ? '/auth/register' : '/auth/login'"
                  class="switch-link"
                >
                  {{ isLogin ? 'Sign up' : 'Sign in' }}
                </router-link>
              </p>
            </div> -->
          <!-- </div>
        </div>
      </div> -->
    </div>
  </template>
  
  <script setup>
  import { computed } from 'vue'
  import { useRoute } from 'vue-router'
  
  const route = useRoute()
  const isLogin = computed(() => route.name === 'login')
  </script>
  
  <style scoped>
  .auth-view {
    min-height: calc(100vh - 5rem);
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 2rem 0;
  }
  
  .auth-container {
    max-width: 500px;
    width: 100%;
    margin: 0 auto;
  }
  
  .auth-card {
    padding: 2rem;
  }
  
  .auth-header {
    text-align: center;
    margin-bottom: 2rem;
  }
  
  .auth-title {
    font-size: 2rem;
    margin-bottom: 0.5rem;
  }
  
  .auth-subtitle {
    color: var(--secondary-text);
    margin-bottom: 2rem;
  }
  
  .auth-footer {
    text-align: center;
    margin-top: 2rem;
    padding-top: 2rem;
    border-top: 1px solid var(--border-color);
  }
  
  .switch-text {
    color: var(--secondary-text);
  }
  
  .switch-link {
    color: var(--accent-color);
    font-weight: 500;
    transition: var(--transition);
  }
  
  .switch-link:hover {
    color: var(--hover-accent);
  }
  </style>