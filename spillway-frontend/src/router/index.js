import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import HomeView from '../views/HomeView.vue'
import VideosView from '../views/VideosView.vue'
import VideoView from '../views/VideoView.vue'
import UploadView from '../views/UploadView.vue'
import PlaylistsView from '../views/PlaylistsView.vue'
import AuthView from '../views/AuthView.vue'
import SharedVideosView from '../views/SharedVideosView.vue'
import EmailConfirmationView from '../views/EmailConfirmationView.vue'
import EncryptionKeyManager from '../views/EncryptionKeyManager.vue'
import LoginForm from '../components/LoginForm.vue'
import RegisterForm from '../components/RegisterForm.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: HomeView
    },
    {
      path: '/videos',
      name: 'videos',
      component: VideosView,
      meta: { requiresAuth: true }
    },
    {
      path: '/video/:id',
      name: 'video',
      component: VideoView,
      props: true
    },
    {
      path: '/upload',
      name: 'upload',
      component: UploadView,
      meta: { requiresAuth: true }
    },
    {
      path: '/playlists',
      name: 'playlists',
      component: PlaylistsView,
      meta: { requiresAuth: true }
    },
    {
      path: '/auth',
      component: AuthView,
      children: [
        {
          path: 'login',
          name: 'login',
          component: LoginForm
        },
        {
          path: 'register',
          name: 'register',
          component: RegisterForm
        }
      ]
    },
    {
      path: '/shared',
      name: 'shared',
      component: SharedVideosView,
      meta: { requiresAuth: true }
    },
    {
      path: '/confirm-email',
      name: 'confirm-email',
      component: EmailConfirmationView
    },
    {
      path: '/encryption-manager',
      name: 'encryption-manager',
      component: EncryptionKeyManager,
      meta: { requiresAuth: true }
    }
  ]
})

// Navigation guard to check auth state
router.beforeEach((to, from, next) => {
  const authStore = useAuthStore()
  
  // If the route requires authentication and user is not logged in
  if (to.meta.requiresAuth && !authStore.isAuthenticated) {
    return next({ name: 'auth', query: { redirect: to.fullPath } })
  }
  
  // If the route is for guests only and user is logged in
  if (to.meta.guestOnly && authStore.isAuthenticated) {
    return next({ name: 'home' })
  }
  
  // Otherwise proceed normally
  next()
})

export default router